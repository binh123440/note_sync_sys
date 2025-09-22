package server;

import common.models.Note;
import common.utils.LoggerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manager để quản lý tất cả notes trên server
 */
public class NoteManager {
    private static final Logger logger = LoggerUtil.getLogger(NoteManager.class);
    
    private final Map<String, Note> notes;
    private final AtomicLong currentVersion;
    
    public NoteManager() {
        this.notes = new ConcurrentHashMap<>();
        this.currentVersion = new AtomicLong(0);
    }
    
    /**
     * Thêm note mới
     */
    public void addNote(Note note) {
        if (note == null || note.getId() == null) {
            throw new IllegalArgumentException("Note and note ID cannot be null");
        }
        
        notes.put(note.getId(), note);
        incrementVersion();
        
        logger.info("Note added: " + note.getId() + " by " + note.getAuthorId());
    }
    
    /**
     * Cập nhật note
     */
    public void updateNote(Note note) {
        if (note == null || note.getId() == null) {
            throw new IllegalArgumentException("Note and note ID cannot be null");
        }
        
        Note existingNote = notes.get(note.getId());
        if (existingNote != null) {
            // Check version conflict
            if (note.getVersion() < existingNote.getVersion()) {
                logger.warning("Version conflict for note: " + note.getId());
                // In a real implementation, you might want to handle conflicts differently
                return;
            }
            
            note.updateLastModified();
            notes.put(note.getId(), note);
            incrementVersion();
            
            logger.info("Note updated: " + note.getId() + " by " + note.getAuthorId());
        } else {
            logger.warning("Attempt to update non-existent note: " + note.getId());
        }
    }
    
    /**
     * Xóa note
     */
    public boolean deleteNote(String noteId) {
        if (noteId == null) {
            throw new IllegalArgumentException("Note ID cannot be null");
        }
        
        Note removedNote = notes.remove(noteId);
        if (removedNote != null) {
            incrementVersion();
            logger.info("Note deleted: " + noteId);
            return true;
        } else {
            logger.warning("Attempt to delete non-existent note: " + noteId);
            return false;
        }
    }
    
    /**
     * Lấy note theo ID
     */
    public Note getNote(String noteId) {
        return notes.get(noteId);
    }
    
    /**
     * Lấy tất cả notes
     */
    public List<Note> getAllNotes() {
        return new ArrayList<>(notes.values());
    }
    
    /**
     * Lấy notes theo author
     */
    public List<Note> getNotesByAuthor(String authorId) {
        return notes.values().stream()
                .filter(note -> Objects.equals(note.getAuthorId(), authorId))
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy notes sau một version cụ thể
     */
    public List<Note> getNotesAfterVersion(long version) {
        return notes.values().stream()
                .filter(note -> note.getVersion() > version)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy notes gần đây nhất
     */
    public List<Note> getRecentNotes(int limit) {
        return notes.values().stream()
                .sorted((n1, n2) -> n2.getLastModified().compareTo(n1.getLastModified()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Tìm kiếm notes theo title hoặc content
     */
    public List<Note> searchNotes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllNotes();
        }
        
        String lowerQuery = query.toLowerCase();
        return notes.values().stream()
                .filter(note -> 
                    (note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) ||
                    (note.getContent() != null && note.getContent().toLowerCase().contains(lowerQuery))
                )
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy version hiện tại
     */
    public long getCurrentVersion() {
        return currentVersion.get();
    }
    
    /**
     * Tăng version
     */
    private void incrementVersion() {
        currentVersion.incrementAndGet();
    }
    
    /**
     * Lấy số lượng notes
     */
    public int getNoteCount() {
        return notes.size();
    }
    
    /**
     * Kiểm tra note có tồn tại không
     */
    public boolean noteExists(String noteId) {
        return notes.containsKey(noteId);
    }
    
    /**
     * Clear tất cả notes (for testing)
     */
    public void clearAllNotes() {
        notes.clear();
        currentVersion.set(0);
        logger.info("All notes cleared");
    }
    
    /**
     * Lấy thống kê
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNotes", notes.size());
        stats.put("currentVersion", currentVersion.get());
        
        Map<String, Long> authorStats = notes.values().stream()
                .collect(Collectors.groupingBy(
                    note -> note.getAuthorId() != null ? note.getAuthorId() : "Unknown",
                    Collectors.counting()
                ));
        stats.put("notesByAuthor", authorStats);
        
        return stats;
    }
}