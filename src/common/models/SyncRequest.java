package common.models;

import java.io.Serializable;
import java.util.List;

/**
 * Lớp để yêu cầu đồng bộ dữ liệu
 */
public class SyncRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String clientId;
    private long lastSyncVersion;
    private List<String> requestedNoteIds;
    private boolean fullSync;
    
    public SyncRequest() {
        this.fullSync = false;
    }
    
    public SyncRequest(String clientId, long lastSyncVersion) {
        this();
        this.clientId = clientId;
        this.lastSyncVersion = lastSyncVersion;
    }
    
    public SyncRequest(String clientId, List<String> requestedNoteIds) {
        this();
        this.clientId = clientId;
        this.requestedNoteIds = requestedNoteIds;
    }
    
    // Getters
    public String getClientId() { return clientId; }
    public long getLastSyncVersion() { return lastSyncVersion; }
    public List<String> getRequestedNoteIds() { return requestedNoteIds; }
    public boolean isFullSync() { return fullSync; }
    
    // Setters
    public void setClientId(String clientId) { this.clientId = clientId; }
    public void setLastSyncVersion(long lastSyncVersion) { this.lastSyncVersion = lastSyncVersion; }
    public void setRequestedNoteIds(List<String> requestedNoteIds) { this.requestedNoteIds = requestedNoteIds; }
    public void setFullSync(boolean fullSync) { this.fullSync = fullSync; }
    
    @Override
    public String toString() {
        return String.format("SyncRequest{clientId='%s', lastVersion=%d, fullSync=%s}", 
                           clientId, lastSyncVersion, fullSync);
    }
}