package common.network;

import common.models.Message;
import java.io.*;

/**
 * Utility class để serialize và deserialize Message objects
 */
public class MessageSerializer {
    
    /**
     * Serialize Message object thành byte array
     */
    public static byte[] serialize(Message message) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(message);
            oos.flush();
            return baos.toByteArray();
        }
    }
    
    /**
     * Deserialize byte array thành Message object
     */
    public static Message deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Message) ois.readObject();
        }
    }
    
    /**
     * Serialize Message object thành OutputStream
     */
    public static void serialize(Message message, OutputStream outputStream) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
            oos.writeObject(message);
            oos.flush();
        }
    }
    
    /**
     * Deserialize Message object từ InputStream
     */
    public static Message deserialize(InputStream inputStream) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(inputStream)) {
            return (Message) ois.readObject();
        }
    }
    
    /**
     * Tính toán kích thước của message sau khi serialize
     */
    public static int getSerializedSize(Message message) throws IOException {
        return serialize(message).length;
    }
}