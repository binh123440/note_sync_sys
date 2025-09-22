package server;

import common.models.*;
import common.network.*;
import common.utils.*;
import java.sql.SQLException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Note Sync Server - Server chính để quản lý đồng bộ notes
 */
public class NoteSyncServer {
    private static final Logger logger = LoggerUtil.getLogger(NoteSyncServer.class);
    
    private final ConfigManager config;
    private final NoteManager noteManager;
    private final ClientManager clientManager;
    
    private ServerSocket tcpServerSocket;
    private UDPConnection udpConnection;
    private boolean isRunning;
    private final ExecutorService threadPool;
    
    public NoteSyncServer() {
        this.config = ConfigManager.getInstance();
        try {
            this.noteManager = new NoteManager();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
        this.clientManager = new ClientManager();
        this.threadPool = Executors.newCachedThreadPool();
        this.isRunning = false;
    }
    
    public void start() throws IOException {
        if (isRunning) {
            logger.warning("Server is already running");
            return;
        }
        
        logger.info("Starting Note Sync Server...");
        
        // Start TCP server
        startTCPServer();
        
        // Start UDP server
        startUDPServer();
        
        // Start background tasks
        startHeartbeatChecker();
        
        isRunning = true;
        logger.info("Note Sync Server started successfully");
        logger.info("TCP Port: " + config.getTcpPort());
        logger.info("UDP Port: " + config.getUdpPort());
    }
    
    private void startTCPServer() throws IOException {
        tcpServerSocket = new ServerSocket(config.getTcpPort());
        isRunning = true; // Set running before starting the loop
        
        // Accept TCP connections
        threadPool.submit(() -> {
            while (isRunning && !tcpServerSocket.isClosed()) {
                try {
                    Socket clientSocket = tcpServerSocket.accept();
                    logger.info("New TCP connection from: " + clientSocket.getRemoteSocketAddress());
                    
                    // Handle client connection in a new thread to avoid blocking the accept loop
                    threadPool.submit(() -> {
                        try {
                            TCPConnection connection = new TCPConnection(clientSocket);
                            handleTCPClient(connection);
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Failed to establish connection with " + clientSocket.getRemoteSocketAddress(), e);
                            try {
                                clientSocket.close();
                            } catch (IOException ioException) {
                                // Ignore
                            }
                        }
                    });
                    
                } catch (IOException e) {
                    if (isRunning) {
                        logger.log(Level.SEVERE, "Error accepting TCP connection", e);
                    }
                }
            }
        });
    }
    
    private void startUDPServer() throws IOException {
        udpConnection = new UDPConnection(config.getUdpPort());
        udpConnection.setMessageHandler(new UDPConnection.MessageHandler() {
            @Override
            public void handleMessage(Message message, java.net.InetAddress sender, int senderPort) {
                handleUDPMessage(message, sender, senderPort);
            }
            
            @Override
            public void onError(Exception e) {
                logger.log(Level.WARNING, "UDP error", e);
            }
        });
        
        udpConnection.startListening();
    }
    
    private void handleTCPClient(TCPConnection connection) {
        connection.setMessageHandler(new TCPConnection.MessageHandler() {
            @Override
            public void handleMessage(Message message) {
                handleTCPMessage(message, connection);
            }
            
            @Override
            public void onConnectionClosed() {
                clientManager.removeClientByConnection(connection);
                logger.info("TCP client disconnected: " + connection.getRemoteAddress());
            }
            
            @Override
            public void onConnectionError(Exception e) {
                logger.log(Level.WARNING, "TCP client error: " + connection.getRemoteAddress(), e);
                clientManager.removeClientByConnection(connection);
            }
        });
        
        connection.startCommunication();
    }
    
    private void handleTCPMessage(Message message, TCPConnection connection) {
        try {
            logger.info("Handling TCP message: " + message.getType() + " from " + message.getSenderId());
            
            switch (message.getType()) {
                case CLIENT_CONNECT:
                    handleClientConnect(message, connection);
                    break;
                case CLIENT_DISCONNECT:
                    handleClientDisconnect(message, connection);
                    break;
                case NOTE_CREATE:
                    handleNoteCreate(message);
                    break;
                case NOTE_UPDATE:
                    handleNoteUpdate(message);
                    break;
                case NOTE_DELETE:
                    handleNoteDelete(message);
                    break;
                case SYNC_REQUEST:
                    handleSyncRequest(message, connection);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(message, connection);
                    break;
                default:
                    logger.warning("Unknown message type: " + message.getType());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling TCP message", e);
            sendErrorMessage(connection, "Error processing message: " + e.getMessage());
        }
    }
    
    private void handleUDPMessage(Message message, java.net.InetAddress sender, int senderPort) {
        try {
            logger.info("Handling UDP message: " + message.getType() + " from " + sender + ":" + senderPort);
            
            switch (message.getType()) {
                case HEARTBEAT:
                    handleUDPHeartbeat(message, sender, senderPort);
                    break;
                case SYNC_REQUEST:
                    handleUDPSyncRequest(message, sender, senderPort);
                    break;
                default:
                    logger.warning("Unsupported UDP message type: " + message.getType());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling UDP message", e);
        }
    }
    
    private void handleClientConnect(Message message, TCPConnection connection) {
        ClientInfo clientInfo = message.getPayload(ClientInfo.class);
        if (clientInfo != null) {
            clientManager.addClient(clientInfo, connection);
            
            // Send connection acknowledgment
            Message ackMessage = new Message(MessageType.CONNECT_ACK, "SERVER", "Connected successfully");
            connection.sendMessage(ackMessage);
            
            // Send current notes to client
            List<Note> allNotes = noteManager.getAllNotes();
            SyncResponse syncResponse = new SyncResponse(clientInfo.getClientId(), allNotes, noteManager.getCurrentVersion());
            Message syncMessage = new Message(MessageType.SYNC_RESPONSE, "SERVER", syncResponse);
            connection.sendMessage(syncMessage);
            
            logger.info("Client connected: " + clientInfo);
        }
    }
    
    private void handleClientDisconnect(Message message, TCPConnection connection) {
        clientManager.removeClientByConnection(connection);
        connection.close();
    }
    
    private void handleNoteCreate(Message message) {
        Note note = message.getPayload(Note.class);
        if (note != null) {
            noteManager.addNote(note);
            
            // Broadcast to all clients
            broadcastNoteChange(MessageType.NOTE_CREATED, note, message.getSenderId());
            logger.info("Note created: " + note.getId());
        }
    }
    
    private void handleNoteUpdate(Message message) {
        Note note = message.getPayload(Note.class);
        if (note != null) {
            noteManager.updateNote(note);
            
            // Broadcast to all clients
            broadcastNoteChange(MessageType.NOTE_UPDATED, note, message.getSenderId());
            logger.info("Note updated: " + note.getId());
        }
    }
    
    private void handleNoteDelete(Message message) {
        String noteId = (String) message.getPayload();
        if (noteId != null) {
            noteManager.deleteNote(noteId);
            
            // Broadcast to all clients
            broadcastNoteChange(MessageType.NOTE_DELETED, noteId, message.getSenderId());
            logger.info("Note deleted: " + noteId);
        }
    }
    
    private void handleSyncRequest(Message message, TCPConnection connection) {
        SyncRequest syncRequest = message.getPayload(SyncRequest.class);
        if (syncRequest != null) {
            List<Note> notes;
            
            if (syncRequest.isFullSync()) {
                notes = noteManager.getAllNotes();
            } else {
                notes = noteManager.getNotesAfterVersion(syncRequest.getLastSyncVersion());
            }
            
            SyncResponse syncResponse = new SyncResponse(syncRequest.getClientId(), notes, noteManager.getCurrentVersion());
            Message responseMessage = new Message(MessageType.SYNC_RESPONSE, "SERVER", syncResponse);
            connection.sendMessage(responseMessage);
            
            logger.info("Sync request handled for client: " + syncRequest.getClientId());
        }
    }
    
    private void handleHeartbeat(Message message, TCPConnection connection) {
        ClientInfo client = clientManager.getClientByConnection(connection);
        if (client != null) {
            client.updateLastSeen();
            
            // Send heartbeat acknowledgment
            Message ackMessage = new Message(MessageType.HEARTBEAT_ACK, "SERVER", "OK");
            connection.sendMessage(ackMessage);
        }
    }
    
    private void handleUDPHeartbeat(Message message, java.net.InetAddress sender, int senderPort) {
        try {
            ClientInfo client = clientManager.getClientById(message.getSenderId());
            if (client != null) {
                client.updateLastSeen();
                
                // Send UDP heartbeat acknowledgment
                Message ackMessage = new Message(MessageType.HEARTBEAT_ACK, "SERVER", "OK");
                udpConnection.sendMessage(ackMessage, sender, senderPort);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error sending UDP heartbeat ack", e);
        }
    }
    
    private void handleUDPSyncRequest(Message message, java.net.InetAddress sender, int senderPort) {
        // Handle UDP sync request (simpler than TCP)
        try {
            List<Note> recentNotes = noteManager.getRecentNotes(10);
            SyncResponse syncResponse = new SyncResponse(message.getSenderId(), recentNotes, noteManager.getCurrentVersion());
            Message responseMessage = new Message(MessageType.SYNC_RESPONSE, "SERVER", syncResponse);
            udpConnection.sendMessage(responseMessage, sender, senderPort);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error sending UDP sync response", e);
        }
    }
    
    private void broadcastNoteChange(MessageType messageType, Object payload, String excludeClientId) {
        Message broadcastMessage = new Message(messageType, "SERVER", payload);
        clientManager.broadcastMessage(broadcastMessage, excludeClientId);
    }
    
    private void sendErrorMessage(TCPConnection connection, String errorMessage) {
        Message errorMsg = new Message(MessageType.ERROR, "SERVER", errorMessage);
        connection.sendMessage(errorMsg);
    }
    
    private void startHeartbeatChecker() {
        threadPool.submit(() -> {
            while (isRunning) {
                try {
                    clientManager.checkClientHeartbeats();
                    Thread.sleep(config.getHeartbeatInterval());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    public void stop() {
        if (!isRunning) return;
        logger.info("Stopping Note Sync Server...");
        isRunning = false;
        
        // Close TCP server socket
        if (tcpServerSocket != null && !tcpServerSocket.isClosed()) {
            try {
                tcpServerSocket.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing TCP server socket", e);
            }
        }
        
        // Stop UDP connection
        if (udpConnection != null) {
            udpConnection.stop();
        }
        
        // Disconnect all clients
        clientManager.disconnectAllClients();
        
        // Shutdown thread pool
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
        
        if (noteManager != null) {
            noteManager.close();
        }
        
        logger.info("Note Sync Server stopped");
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public static void main(String[] args) {
        LoggerUtil.initializeLogging();
        
        NoteSyncServer server = new NoteSyncServer();
        
        try {
            server.start();
            
            // Keep server running
            Scanner scanner = new Scanner(System.in);
            System.out.println("Server is running. Type 'quit' to stop:");
            
            while (server.isRunning()) {
                String input = scanner.nextLine();
                if ("quit".equalsIgnoreCase(input.trim())) {
                    break;
                }
            }
            
            scanner.close();
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to start server", e);
        } finally {
            server.stop();
        }
    }
}