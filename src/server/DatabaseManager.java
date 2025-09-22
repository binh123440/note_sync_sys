package server;

import common.models.Note;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:./data/notesync;AUTO_SERVER=TRUE";
    private Connection connection;
    
    public DatabaseManager() throws SQLException {
        try {
            // Tải H2 driver
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            // Chuyển đổi exception để không cần thay đổi signature của các phương thức gọi nó
            throw new SQLException("H2 Driver not found. Please add h2.jar to your classpath.", e);
        }
        connection = DriverManager.getConnection(DB_URL, "sa", "");
        createTables();
    }
    
    private void createTables() throws SQLException {
        // Thay thế text block bằng String nối chuỗi để tương thích với Java cũ hơn
        String createTableSQL = "CREATE TABLE IF NOT EXISTS notes ("
                + "id VARCHAR(255) PRIMARY KEY,"
                + "title VARCHAR(1000),"
                + "content CLOB,"
                + "author_id VARCHAR(255),"
                + "created_at TIMESTAMP,"
                + "last_modified TIMESTAMP,"
                + "version BIGINT DEFAULT 1"
                + ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }
    
    public void saveNote(Note note) throws SQLException {
        String sql = "MERGE INTO notes VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, note.getId());
            pstmt.setString(2, note.getTitle());
            pstmt.setString(3, note.getContent());
            pstmt.setString(4, note.getAuthorId());
            pstmt.setTimestamp(5, Timestamp.valueOf(note.getCreatedAt()));
            pstmt.setTimestamp(6, Timestamp.valueOf(note.getLastModified()));
            pstmt.setLong(7, note.getVersion());
            pstmt.executeUpdate();
        }
    }
    
    public List<Note> getAllNotes() throws SQLException {
        List<Note> notes = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM notes")) {
            while (rs.next()) {
                Note note = new Note();
                note.setId(rs.getString("id"));
                note.setTitle(rs.getString("title"));
                note.setContent(rs.getString("content"));
                note.setAuthorId(rs.getString("author_id"));
                note.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                note.setLastModified(rs.getTimestamp("last_modified").toLocalDateTime());
                note.setVersion(rs.getLong("version"));
                notes.add(note);
            }
        }
        return notes;
    }
    
    public boolean deleteNote(String id) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM notes WHERE id = ?")) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException e) {}
    }
}
