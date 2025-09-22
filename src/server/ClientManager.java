package server;

import common.models.ClientInfo;
import common.models.Message;
import common.network.TCPConnection;
import common.utils.LoggerUtil;
import common.utils.ConfigManager;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manager để quản lý tất cả clients kết nối tới server
 */
public class ClientManager {
    private static final Logger logger = LoggerUtil.getLogger(ClientManager.class);
    
    private final Map<String, ClientInfo> clients;
    private final Map<String, TCPConnection> clientConnections;
    private final Map<TCPConnection, String> connectionToClientId;
    private final ConfigManager config;
    
    public ClientManager() {
        this.clients = new ConcurrentHashMap<>();
        this.clientConnections = new ConcurrentHashMap<>();
        this.connectionToClientId = new ConcurrentHashMap<>();
        this.config = ConfigManager.getInstance();
    }
    
    /**
     * Thêm client mới
     */
    public void addClient(ClientInfo clientInfo, TCPConnection connection) {
        if (clientInfo == null || connection == null) {
            throw new IllegalArgumentException("Client info and connection cannot be null");
        }
        
        String clientId = clientInfo.getClientId();
        
        // Remove old connection if exists
        removeClient(clientId);
        
        // Add new client
        clients.put(clientId, clientInfo);
        clientConnections.put(clientId, connection);
        connectionToClientId.put(connection, clientId);
        
        logger.info("Client added: " + clientInfo);
        logger.info("Total active clients: " + clients.size());
    }
    
    /**
     * Xóa client theo ID
     */
    public void removeClient(String clientId) {
        if (clientId == null) return;
        
        ClientInfo client = clients.remove(clientId);
        TCPConnection connection = clientConnections.remove(clientId);
        
        if (connection != null) {
            connectionToClientId.remove(connection);
            if (connection.isConnected()) {
                connection.close();
            }
        }
        
        if (client != null) {
            logger.info("Client removed: " + client);
            logger.info("Total active clients: " + clients.size());
        }
    }
    
    /**
     * Xóa client theo connection
     */
    public void removeClientByConnection(TCPConnection connection) {
        if (connection == null) return;
        
        String clientId = connectionToClientId.remove(connection);
        if (clientId != null) {
            clients.remove(clientId);
            clientConnections.remove(clientId);
            
            logger.info("Client removed by connection: " + clientId);
            logger.info("Total active clients: " + clients.size());
        }
    }
    
    /**
     * Lấy client theo ID
     */
    public ClientInfo getClientById(String clientId) {
        return clients.get(clientId);
    }
    
    /**
     * Lấy client theo connection
     */
    public ClientInfo getClientByConnection(TCPConnection connection) {
        String clientId = connectionToClientId.get(connection);
        return clientId != null ? clients.get(clientId) : null;
    }
    
    /**
     * Lấy connection của client
     */
    public TCPConnection getClientConnection(String clientId) {
        return clientConnections.get(clientId);
    }
    
    /**
     * Lấy tất cả clients
     */
    public List<ClientInfo> getAllClients() {
        return new ArrayList<>(clients.values());
    }
    
    /**
     * Lấy clients online
     */
    public List<ClientInfo> getOnlineClients() {
        return clients.values().stream()
                .filter(ClientInfo::isOnline)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Broadcast message tới tất cả clients
     */
    public void broadcastMessage(Message message) {
        broadcastMessage(message, null);
    }
    
    /**
     * Broadcast message tới tất cả clients trừ client bị loại trừ
     */
    public void broadcastMessage(Message message, String excludeClientId) {
        List<String> failedClients = new ArrayList<>();
        
        for (Map.Entry<String, TCPConnection> entry : clientConnections.entrySet()) {
            String clientId = entry.getKey();
            TCPConnection connection = entry.getValue();
            
            // Skip excluded client
            if (clientId.equals(excludeClientId)) {
                continue;
            }
            
            try {
                if (connection.isConnected()) {
                    connection.sendMessage(message);
                } else {
                    failedClients.add(clientId);
                }
            } catch (Exception e) {
                logger.warning("Failed to send message to client " + clientId + ": " + e.getMessage());
                failedClients.add(clientId);
            }
        }
        
        // Remove failed clients
        for (String clientId : failedClients) {
            removeClient(clientId);
        }
        
        logger.info("Broadcasted message type " + message.getType() + " to " + 
                   (clientConnections.size() - failedClients.size()) + " clients");
    }
    
    /**
     * Gửi message tới client cụ thể
     */
    public boolean sendMessageToClient(String clientId, Message message) {
        TCPConnection connection = clientConnections.get(clientId);
        if (connection != null && connection.isConnected()) {
            try {
                connection.sendMessage(message);
                return true;
            } catch (Exception e) {
                logger.warning("Failed to send message to client " + clientId + ": " + e.getMessage());
                removeClient(clientId);
                return false;
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra heartbeat của tất cả clients
     */
    public void checkClientHeartbeats() {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(
            config.getHeartbeatInterval() * 2, ChronoUnit.MILLIS);
        
        List<String> disconnectedClients = new ArrayList<>();
        
        for (ClientInfo client : clients.values()) {
            if (client.getLastSeen().isBefore(cutoffTime)) {
                disconnectedClients.add(client.getClientId());
                client.setOnline(false);
            }
        }
        
        // Remove disconnected clients
        for (String clientId : disconnectedClients) {
            logger.info("Client timeout detected: " + clientId);
            removeClient(clientId);
        }
        
        if (!disconnectedClients.isEmpty()) {
            logger.info("Removed " + disconnectedClients.size() + " inactive clients");
        }
    }
    
    /**
     * Ngắt kết nối tất cả clients
     */
    public void disconnectAllClients() {
        logger.info("Disconnecting all clients...");
        
        for (TCPConnection connection : clientConnections.values()) {
            try {
                if (connection.isConnected()) {
                    connection.close();
                }
            } catch (Exception e) {
                logger.warning("Error disconnecting client: " + e.getMessage());
            }
        }
        
        clients.clear();
        clientConnections.clear();
        connectionToClientId.clear();
        
        logger.info("All clients disconnected");
    }
    
    /**
     * Lấy số lượng clients
     */
    public int getClientCount() {
        return clients.size();
    }
    
    /**
     * Lấy số lượng clients online
     */
    public int getOnlineClientCount() {
        return (int) clients.values().stream()
                .filter(ClientInfo::isOnline)
                .count();
    }
    
    /**
     * Kiểm tra client có tồn tại không
     */
    public boolean clientExists(String clientId) {
        return clients.containsKey(clientId);
    }
    
    /**
     * Lấy thống kê clients
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClients", clients.size());
        stats.put("onlineClients", getOnlineClientCount());
        stats.put("maxClients", config.getMaxClients());
        
        List<Map<String, Object>> clientList = new ArrayList<>();
        for (ClientInfo client : clients.values()) {
            Map<String, Object> clientData = new HashMap<>();
            clientData.put("id", client.getClientId());
            clientData.put("name", client.getClientName());
            clientData.put("online", client.isOnline());
            clientData.put("lastSeen", client.getLastSeen().toString());
            clientList.add(clientData);
        }
        stats.put("clients", clientList);
        
        return stats;
    }
}