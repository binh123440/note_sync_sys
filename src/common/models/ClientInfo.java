package common.models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một client trong hệ thống
 */
public class ClientInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String clientId;
    private String clientName;
    private String ipAddress;
    private int port;
    private LocalDateTime lastSeen;
    private boolean isOnline;
    
    public ClientInfo() {
        this.lastSeen = LocalDateTime.now();
        this.isOnline = true;
    }
    
    public ClientInfo(String clientId, String clientName, String ipAddress, int port) {
        this();
        this.clientId = clientId;
        this.clientName = clientName;
        this.ipAddress = ipAddress;
        this.port = port;
    }
    
    // Getters
    public String getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getIpAddress() { return ipAddress; }
    public int getPort() { return port; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public boolean isOnline() { return isOnline; }
    
    // Setters
    public void setClientId(String clientId) { this.clientId = clientId; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setPort(int port) { this.port = port; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    public void setOnline(boolean online) { this.isOnline = online; }
    
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
        this.isOnline = true;
    }
    
    @Override
    public String toString() {
        return String.format("ClientInfo{id='%s', name='%s', ip='%s:%d', online=%s}", 
                           clientId, clientName, ipAddress, port, isOnline);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ClientInfo that = (ClientInfo) obj;
        return clientId != null ? clientId.equals(that.clientId) : that.clientId == null;
    }
    
    @Override
    public int hashCode() {
        return clientId != null ? clientId.hashCode() : 0;
    }
}