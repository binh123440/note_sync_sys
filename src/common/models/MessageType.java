package common.models;

import java.io.Serializable;

/**
 * Enum định nghĩa các loại message trong hệ thống
 */
public enum MessageType implements Serializable {
    // Client -> Server messages
    CLIENT_CONNECT,      // Client kết nối đến server
    CLIENT_DISCONNECT,   // Client ngắt kết nối
    NOTE_CREATE,         // Tạo note mới
    NOTE_UPDATE,         // Cập nhật note
    NOTE_DELETE,         // Xóa note
    SYNC_REQUEST,        // Yêu cầu đồng bộ
    HEARTBEAT,           // Ping để duy trì kết nối
    
    // Server -> Client messages
    CONNECT_ACK,         // Xác nhận kết nối
    NOTE_SYNC,           // Đồng bộ note
    NOTE_CREATED,        // Thông báo note được tạo
    NOTE_UPDATED,        // Thông báo note được cập nhật
    NOTE_DELETED,        // Thông báo note bị xóa
    SYNC_RESPONSE,       // Phản hồi đồng bộ
    CLIENT_LIST,         // Danh sách client online
    ERROR,               // Thông báo lỗi
    
    // Bidirectional
    HEARTBEAT_ACK        // Phản hồi heartbeat
}