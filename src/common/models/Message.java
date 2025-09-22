package common.models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một message trong hệ thống
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private MessageType type;
    private String senderId;
    private Object payload;
    private LocalDateTime timestamp;
    private String messageId;
    
    public Message() {
        this.timestamp = LocalDateTime.now();
        this.messageId = java.util.UUID.randomUUID().toString();
    }
    
    public Message(MessageType type, String senderId, Object payload) {
        this();
        this.type = type;
        this.senderId = senderId;
        this.payload = payload;
    }
    
    // Getters
    public MessageType getType() { return type; }
    public String getSenderId() { return senderId; }
    public Object getPayload() { return payload; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMessageId() { return messageId; }
    
    // Setters
    public void setType(MessageType type) { this.type = type; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setPayload(Object payload) { this.payload = payload; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    // Utility methods
    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> clazz) {
        if (payload != null && clazz.isAssignableFrom(payload.getClass())) {
            return (T) payload;
        }
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("Message{type=%s, sender='%s', id='%s', timestamp=%s}", 
                           type, senderId, messageId, timestamp);
    }
}