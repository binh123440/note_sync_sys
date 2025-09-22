package server;

import common.models.Note;
import common.utils.LoggerUtil;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manager để quản lý tất cả notes trên server bằng Database
 */
public class NoteManager {
    private static final Logger logger = LoggerUtil.getLogger(NoteManager.class);
    private final DatabaseManager databaseManager;
    // currentVersion sẽ được quản lý bởi logic trong DB trong tương lai,
    // tạm thời vẫn giữ để đảm bảo các chức năng khác hoạt động
    private final AtomicLong currentVersion; 

    public NoteManager() throws SQLException {
        this.databaseManager = new DatabaseManager();
        // Khởi tạo version từ DB hoặc bắt đầu từ 0
        this.currentVersion = new AtomicLong(0); 
    }

    /**
     * Thêm note mới
     */
    public void addNote(Note note) {
        try {
            databaseManager.saveNote(note);
            incrementVersion();
            logger.info("Note added: " + note.getId());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error adding note", e);
            throw new RuntimeException("Failed to add note", e);
        }
    }

    /**
     * Cập nhật note
     */
    public void updateNote(Note note) {
        if (note == null || note.getId() == null) {
            throw new IllegalArgumentException("Note and note ID cannot be null");
        }
        
        try {
            // Lấy note hiện có từ DB để kiểm tra
            Note existingNote = getNote(note.getId());
            if (existingNote != null) {
                if (note.getVersion() < existingNote.getVersion()) {
                    logger.warning("Version conflict for note: " + note.getId());
                    return;
                }
                
                note.updateLastModified();
                databaseManager.saveNote(note); // Dùng saveNote cho cả update (MERGE)
                incrementVersion();
                
                logger.info("Note updated: " + note.getId() + " by " + note.getAuthorId());
            } else {
                logger.warning("Attempt to update non-existent note: " + note.getId());
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating note", e);
            throw new RuntimeException("Failed to update note", e);
        }
    }

    /**
     * Xóa note
     */
    public boolean deleteNote(String noteId) {
        try {
            boolean deleted = databaseManager.deleteNote(noteId);
            if (deleted) {
                incrementVersion();
                logger.info("Note deleted: " + noteId);
            }
            return deleted;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting note", e);
            return false;
        }
    }

    /**
     * Lấy note theo ID từ DB
     */
    public Note getNote(String noteId) {
        try {
            // Giả sử bạn sẽ thêm phương thức getNoteById vào DatabaseManager
            List<Note> allNotes = databaseManager.getAllNotes();
            return allNotes.stream()
                           .filter(n -> n.getId().equals(noteId))
                           .findFirst()
                           .orElse(null);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting note by ID", e);
            return null;
        }
    }

    /**
     * Lấy tất cả notes từ DB
     */
    public List<Note> getAllNotes() {
        try {
            return databaseManager.getAllNotes();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting all notes", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy notes theo author từ DB
     */
    public List<Note> getNotesByAuthor(String authorId) {
        try {
            return databaseManager.getAllNotes().stream()
                    .filter(note -> Objects.equals(note.getAuthorId(), authorId))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting notes by author", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy notes sau một version cụ thể
     */
    public List<Note> getNotesAfterVersion(long version) {
        try {
            return databaseManager.getAllNotes().stream()
                    .filter(note -> note.getVersion() > version)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting notes after version", e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy notes gần đây nhất
     */
    public List<Note> getRecentNotes(int limit) {
        try {
            return databaseManager.getAllNotes().stream()
                    .sorted(Comparator.comparing(Note::getLastModified).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting recent notes", e);
            return new ArrayList<>();
        }
    }

    /**
     * Tìm kiếm notes theo title hoặc content
     */
    public List<Note> searchNotes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllNotes();
        }
        
        String lowerQuery = query.toLowerCase();
        try {
            return databaseManager.getAllNotes().stream()
                    .filter(note -> 
                        (note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerQuery)) ||
                        (note.getContent() != null && note.getContent().toLowerCase().contains(lowerQuery))
                    )
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error searching notes", e);
            return new ArrayList<>();
        }
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
        try {
            return databaseManager.getAllNotes().size();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting note count", e);
            return 0;
        }
    }

    /**
     * Kiểm tra note có tồn tại không
     */
    public boolean noteExists(String noteId) {
        return getNote(noteId) != null;
    }

    /**
     * Clear tất cả notes (for testing)
     */
    public void clearAllNotes() {
        try {
            List<Note> allNotes = databaseManager.getAllNotes();
            for (Note note : allNotes) {
                databaseManager.deleteNote(note.getId());
            }
            currentVersion.set(0);
            logger.info("All notes cleared");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error clearing all notes", e);
        }
    }

    /**
     * Lấy thống kê
     */
    public Map<String, Object> getStatistics() {
        try {
            List<Note> allNotes = databaseManager.getAllNotes();
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalNotes", allNotes.size());
            stats.put("currentVersion", currentVersion.get());
            
            Map<String, Long> authorStats = allNotes.stream()
                    .collect(Collectors.groupingBy(
                        note -> note.getAuthorId() != null ? note.getAuthorId() : "Unknown",
                        Collectors.counting()
                    ));
            stats.put("notesByAuthor", authorStats);
            
            return stats;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting statistics", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Đóng kết nối DB khi server dừng
     */
    public void close() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}