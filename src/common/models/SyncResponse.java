package common.models;

import java.io.Serializable;
import java.util.List;

/**
 * Lớp để phản hồi yêu cầu đồng bộ
 */
public class SyncResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String clientId;
    private List<Note> notes;
    private List<String> deletedNoteIds;
    private long syncVersion;
    private boolean success;
    private String errorMessage;
    
    public SyncResponse() {
        this.success = true;
    }
    
    public SyncResponse(String clientId, List<Note> notes, long syncVersion) {
        this();
        this.clientId = clientId;
        this.notes = notes;
        this.syncVersion = syncVersion;
    }
    
    // Getters
    public String getClientId() { return clientId; }
    public List<Note> getNotes() { return notes; }
    public List<String> getDeletedNoteIds() { return deletedNoteIds; }
    public long getSyncVersion() { return syncVersion; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    
    // Setters
    public void setClientId(String clientId) { this.clientId = clientId; }
    public void setNotes(List<Note> notes) { this.notes = notes; }
    public void setDeletedNoteIds(List<String> deletedNoteIds) { this.deletedNoteIds = deletedNoteIds; }
    public void setSyncVersion(long syncVersion) { this.syncVersion = syncVersion; }
    public void setSuccess(boolean success) { this.success = success; }
    public void setErrorMessage(String errorMessage) { 
        this.errorMessage = errorMessage;
        this.success = false;
    }
    
    @Override
    public String toString() {
        return String.format("SyncResponse{clientId='%s', notesCount=%d, version=%d, success=%s}", 
                           clientId, notes != null ? notes.size() : 0, syncVersion, success);
    }
}