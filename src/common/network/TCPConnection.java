package common.network;

import common.models.Message;
import common.utils.ConfigManager;
import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * TCP Connection handler để quản lý kết nối TCP
 */
public class TCPConnection {
    private static final Logger logger = Logger.getLogger(TCPConnection.class.getName());
    
    private final Socket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private volatile boolean isConnected;
    private final BlockingQueue<Message> messageQueue;
    private Thread senderThread;
    private Thread receiverThread;
    private MessageHandler messageHandler;
    
    public interface MessageHandler {
        void handleMessage(Message message);
        void onConnectionClosed();
        void onConnectionError(Exception e);
    }
    
    public TCPConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.messageQueue = new LinkedBlockingQueue<>();
        
        // CRITICAL: Create output stream and flush header BEFORE creating input stream
        // This is the key to preventing deadlocks.
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        
        this.isConnected = true;
    }
    
    public TCPConnection(String host, int port) throws IOException {
        this(createSocketWithTimeout(host, port));
    }
    
    private static Socket createSocketWithTimeout(String host, int port) throws IOException {
        Socket socket = new Socket();
        try {
            // Use timeout from config
            int timeout = ConfigManager.getInstance().getConnectionTimeout();
            socket.connect(new java.net.InetSocketAddress(host, port), timeout);
            return socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }
    
    public void startCommunication() {
        startSenderThread();
        startReceiverThread();
    }
    
    private void startSenderThread() {
        senderThread = new Thread(() -> {
            try {
                while (isConnected && !Thread.currentThread().isInterrupted()) {
                    Message message = messageQueue.take();
                    synchronized (outputStream) {
                        outputStream.writeObject(message);
                        outputStream.flush();
                    }
                    logger.fine("Sent message: " + message.getType());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Sender thread interrupted.");
            } catch (IOException e) {
                if (isConnected) {
                    logger.log(Level.SEVERE, "Error sending message", e);
                    handleConnectionError(e);
                }
            }
        });
        senderThread.setName("TCP-Sender-" + socket.getPort());
        senderThread.start();
    }

    private void startReceiverThread() {
        receiverThread = new Thread(() -> {
            try {
                while (isConnected && !Thread.currentThread().isInterrupted()) {
                    Message message = (Message) inputStream.readObject();
                    logger.info("Received message: " + message.getType() + " from " + getRemoteAddress());
                    if (messageHandler != null) {
                        messageHandler.handleMessage(message);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (isConnected) {
                    logger.log(Level.WARNING, "Error in receiver thread, connection likely closed: " + getRemoteAddress(), e);
                    handleConnectionError(e);
                }
            }
        });
        receiverThread.setName("TCP-Receiver-" + socket.getPort());
        receiverThread.start();
    }
    
    public void sendMessage(Message message) {
        if (!isConnected) {
            logger.warning("Attempted to send message on a closed connection.");
            return;
        }
        
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Interrupted while queuing message", e);
        }
    }
    
    public void sendMessageSync(Message message) throws IOException {
        if (isConnected) {
            synchronized (outputStream) {
                outputStream.writeObject(message);
                outputStream.flush();
            }
        }
    }
    
    private void handleConnectionError(Exception e) {
        if (isConnected) {
            isConnected = false; // Set flag immediately to prevent further operations
            if (messageHandler != null) {
                messageHandler.onConnectionError(e);
            }
            close();
        }
    }
    
    public void close() {
        if (!isConnected) {
            return; // Already closing or closed
        }
        isConnected = false;
        
        // Interrupt threads to unblock them from waiting operations
        if (senderThread != null) senderThread.interrupt();
        if (receiverThread != null) receiverThread.interrupt();
        
        // Close streams and socket safely
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close(); // Closing the socket will cause stream operations to throw exceptions
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error closing socket", e);
        }
        
        // The streams are implicitly closed when the socket is closed.
        // Explicitly closing them can sometimes cause issues if the other end is still writing.
        
        if (messageHandler != null) {
            messageHandler.onConnectionClosed();
        }
        
        logger.info("TCP connection closed for " + getRemoteAddress());
    }
    
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
    
    public String getRemoteAddress() {
        return socket != null ? socket.getRemoteSocketAddress().toString() : "Unknown";
    }
}