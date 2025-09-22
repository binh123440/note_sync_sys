package common.network;

import common.models.Message;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * UDP Connection handler để quản lý giao tiếp UDP
 */
public class UDPConnection {
    private static final Logger logger = Logger.getLogger(UDPConnection.class.getName());
    private static final int MAX_PACKET_SIZE = 65507; // Max UDP packet size
    
    private DatagramSocket socket;
    private boolean isRunning;
    private final ExecutorService executor;
    private MessageHandler messageHandler;
    
    public interface MessageHandler {
        void handleMessage(Message message, InetAddress sender, int senderPort);
        void onError(Exception e);
    }
    
    public UDPConnection() throws SocketException {
        this.socket = new DatagramSocket();
        this.executor = Executors.newCachedThreadPool();
        this.isRunning = false;
    }
    
    public UDPConnection(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.executor = Executors.newCachedThreadPool();
        this.isRunning = false;
    }
    
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }
    
    public void startListening() {
        if (isRunning) return;
        
        isRunning = true;
        executor.submit(this::receiveMessages);
        logger.info("UDP connection started listening on port: " + socket.getLocalPort());
    }
    
    private void receiveMessages() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        
        while (isRunning && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // Deserialize message
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                
                Message message = MessageSerializer.deserialize(data);
                
                logger.info("Received UDP message: " + message.getType() + " from " + 
                          packet.getAddress() + ":" + packet.getPort());
                
                if (messageHandler != null) {
                    executor.submit(() -> messageHandler.handleMessage(
                        message, packet.getAddress(), packet.getPort()));
                }
                
            } catch (IOException | ClassNotFoundException e) {
                if (isRunning) {
                    logger.log(Level.WARNING, "Error receiving UDP message", e);
                    if (messageHandler != null) {
                        messageHandler.onError(e);
                    }
                }
            }
        }
    }
    
    public void sendMessage(Message message, InetAddress address, int port) throws IOException {
        if (!isRunning) {
            throw new IllegalStateException("UDP connection is not running");
        }
        
        byte[] data = MessageSerializer.serialize(message);
        
        if (data.length > MAX_PACKET_SIZE) {
            throw new IOException("Message too large for UDP: " + data.length + " bytes");
        }
        
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
        
        logger.info("Sent UDP message: " + message.getType() + " to " + address + ":" + port);
    }
    
    public void sendMessage(Message message, String host, int port) throws IOException {
        sendMessage(message, InetAddress.getByName(host), port);
    }
    
    public void sendBroadcast(Message message, int port) throws IOException {
        sendMessage(message, InetAddress.getByName("255.255.255.255"), port);
    }
    
    public void stop() {
        isRunning = false;
        
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        
        executor.shutdown();
        logger.info("UDP connection stopped");
    }
    
    public boolean isRunning() {
        return isRunning && socket != null && !socket.isClosed();
    }
    
    public int getLocalPort() {
        return socket != null ? socket.getLocalPort() : -1;
    }
    
    public InetAddress getLocalAddress() {
        return socket != null ? socket.getLocalAddress() : null;
    }
}