package common.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp đại diện cho một ghi chú trong hệ thống
 */
public class Note implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String title;
    private String content;
    private String authorId;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private long version; // Version để đồng bộ
    
    public Note() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.version = 1;
    }
    
    public Note(String title, String content, String authorId) {
        this();
        this.title = title;
        this.content = content;
        this.authorId = authorId;
    }
    
    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthorId() { return authorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastModified() { return lastModified; }
    public long getVersion() { return version; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { 
        this.title = title;
        updateLastModified();
    }
    public void setContent(String content) { 
        this.content = content;
        updateLastModified();
    }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    public void setVersion(long version) { this.version = version; }
    
    // Utility methods
    public void updateLastModified() {
        this.lastModified = LocalDateTime.now();
        this.version++;
    }
    
    public Note copy() {
        Note copy = new Note();
        copy.id = this.id;
        copy.title = this.title;
        copy.content = this.content;
        copy.authorId = this.authorId;
        copy.createdAt = this.createdAt;
        copy.lastModified = this.lastModified;
        copy.version = this.version;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("Note{id='%s', title='%s', author='%s', version=%d}", 
                           id, title, authorId, version);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Note note = (Note) obj;
        return id != null ? id.equals(note.id) : note.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}