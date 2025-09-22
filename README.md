# Hệ thống Đồng bộ Ghi chú (Note Sync System)

## Mô tả
Hệ thống đồng bộ ghi chú là một ứng dụng phân tán được phát triển bằng Java, cho phép nhiều client tạo, chỉnh sửa và đồng bộ ghi chú thông qua một server trung tâm. Hệ thống hỗ trợ cả giao thức TCP và UDP để đảm bảo hiệu suất và độ tin cậy.

## Tính năng chính

### Server
- **Quản lý kết nối**: Hỗ trợ kết nối đồng thời từ nhiều client
- **Đồng bộ real-time**: Tự động đồng bộ thay đổi tới tất cả client
- **Giao thức kép**: Hỗ trợ cả TCP (reliable) và UDP (fast)
- **Heartbeat monitoring**: Theo dõi trạng thái kết nối của client
- **Version control**: Quản lý phiên bản để tránh xung đột

### Client
- **GUI thân thiện**: Giao diện Swing dễ sử dụng
- **CRUD operations**: Tạo, đọc, cập nhật, xóa ghi chú
- **Auto-sync**: Tự động nhận cập nhật từ server
- **Offline support**: Làm việc offline và đồng bộ khi reconnect
- **Real-time updates**: Nhận thay đổi từ client khác ngay lập tức

## Kiến trúc hệ thống

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│     Client 1    │       │     Server      │       │     Client 2    │
│                 │       │                 │       │                 │
│ ┌─────────────┐ │       │ ┌─────────────┐ │       │ ┌─────────────┐ │
│ │     GUI     │ │       │ │NoteManager  │ │       │ │     GUI     │ │
│ └─────────────┘ │       │ └─────────────┘ │       │ └─────────────┘ │
│ ┌─────────────┐ │  TCP  │ ┌─────────────┐ │  TCP  │ ┌─────────────┐ │
│ │TCPConnection│◄├───────┤►│ClientManager│◄├───────┤►│TCPConnection│ │
│ └─────────────┘ │       │ └─────────────┘ │       │ └─────────────┘ │
│ ┌─────────────┐ │  UDP  │ ┌─────────────┐ │  UDP  │ ┌─────────────┐ │
│ │UDPConnection│◄├───────┤►│UDPConnection│◄├───────┤►│UDPConnection│ │
│ └─────────────┘ │       │ └─────────────┘ │       │ └─────────────┘ │
└─────────────────┘       └─────────────────┘       └─────────────────┘
```

## Cấu trúc thư mục

```
NoteSyncSystem/
├── src/
│   ├── common/
│   │   ├── models/          # Data models
│   │   │   ├── Note.java
│   │   │   ├── ClientInfo.java
│   │   │   ├── Message.java
│   │   │   ├── MessageType.java
│   │   │   ├── SyncRequest.java
│   │   │   └── SyncResponse.java
│   │   ├── network/         # Network utilities
│   │   │   ├── MessageSerializer.java
│   │   │   ├── TCPConnection.java
│   │   │   └── UDPConnection.java
│   │   └── utils/           # Utility classes
│   │       ├── ConfigManager.java
│   │       ├── LoggerUtil.java
│   │       └── Utils.java
│   ├── server/              # Server implementation
│   │   ├── NoteSyncServer.java
│   │   ├── NoteManager.java
│   │   └── ClientManager.java
│   └── client/              # Client implementation
│       └── NoteSyncClient.java
├── config.properties        # Configuration file
├── compile.bat             # Windows compilation script
├── run-server.bat          # Windows server startup script
├── run-client.bat          # Windows client startup script
└── README.md
```

## Cài đặt và chạy

### Yêu cầu hệ thống
- Java 8 hoặc cao hơn
- Windows/Linux/MacOS
- Ít nhất 512MB RAM
- Port 8080 (TCP) và 8081 (UDP) không bị chiếm dụng

### Cách nhanh nhất để test:

#### 1. Sử dụng Quick Start:
```cmd
quick-start.bat
```

#### 2. Sử dụng Test Menu:
```cmd
test.bat
```

### Biên dịch thủ công

#### Windows:
```cmd
compile-simple.bat
```

#### Linux/MacOS:
```bash
# Tạo thư mục build
mkdir -p build

# Biên dịch tất cả file Java
find src -name "*.java" -print0 | xargs -0 javac -d build -cp build
```

### Chạy ứng dụng

#### Cách 1: Sử dụng script đơn giản

**1. Khởi động Server:**
```cmd
run-server-simple.bat
```

**2. Khởi động Client (trong cửa sổ command khác):**
```cmd
run-client-simple.bat Alice
run-client-simple.bat Bob
```

#### Cách 2: Sử dụng menu test
```cmd
test.bat
```

#### Cách 3: Chạy thủ công

**Linux/MacOS:**
```bash
# Terminal 1 - Server
cd build
java server.NoteSyncServer

# Terminal 2 - Client
cd build  
java client.NoteSyncClient Alice
```

## Cấu hình

File `config.properties` cho phép tùy chỉnh các thiết lập:

```properties
# Server configuration
server.host=localhost
server.tcp.port=8080
server.udp.port=8081
server.max.clients=100

# Network configuration
network.heartbeat.interval=30000
network.connection.timeout=10000

# Client configuration
client.auto.reconnect=true

# Logging
logging.level=INFO

# Synchronization
sync.batch.size=10
```

## Giao thức truyền thông

### TCP Messages
- `CLIENT_CONNECT`: Client kết nối tới server
- `NOTE_CREATE`: Tạo ghi chú mới
- `NOTE_UPDATE`: Cập nhật ghi chú
- `NOTE_DELETE`: Xóa ghi chú
- `SYNC_REQUEST`: Yêu cầu đồng bộ
- `HEARTBEAT`: Duy trì kết nối

### UDP Messages
- `HEARTBEAT`: Ping nhanh
- `SYNC_REQUEST`: Đồng bộ nhanh

### Message Format
```json
{
  "type": "NOTE_CREATE",
  "senderId": "client-123",
  "payload": {
    "id": "note-456",
    "title": "Tiêu đề",
    "content": "Nội dung",
    "authorId": "client-123",
    "version": 1,
    "createdAt": "2025-09-22T10:30:00",
    "lastModified": "2025-09-22T10:30:00"
  },
  "timestamp": "2025-09-22T10:30:00",
  "messageId": "msg-789"
}
```

## Sử dụng

### Server
1. Chạy server trước
2. Server sẽ lắng nghe trên port TCP 8080 và UDP 8081
3. Gõ 'quit' để dừng server

### Client
1. Nhập tên client khi khởi động
2. Click "Connect" để kết nối tới server
3. Tạo, chỉnh sửa, xóa ghi chú
4. Các thay đổi sẽ được đồng bộ tự động
5. Click "Sync" để đồng bộ thủ công

### Tính năng chính:
- **Tạo ghi chú**: Nhập tiêu đề và nội dung, click "Create"
- **Chỉnh sửa**: Chọn ghi chú từ danh sách, sửa và click "Update"
- **Xóa**: Chọn ghi chú và click "Delete"
- **Đồng bộ**: Tự động hoặc click "Sync"

## Troubleshooting

### Lỗi thường gặp:

1. **Client bị đứng khi bấm "Connect"**
   - **Nguyên nhân**: Server chưa chạy hoặc không thể kết nối
   - **Giải pháp**: 
     - Đảm bảo server đã chạy trước
     - Kiểm tra port 8080, 8081 không bị chặn
     - Chờ 10 giây để timeout, sau đó sẽ hiện thông báo lỗi

2. **Connection refused**
   - Kiểm tra server đã chạy chưa
   - Kiểm tra port không bị chiếm dụng
   - Kiểm tra firewall

3. **ClassNotFoundException**
   - Kiểm tra classpath
   - Đảm bảo đã biên dịch đúng

4. **BindException**
   - Port đã được sử dụng
   - Thay đổi port trong config.properties

5. **Sync issues**
   - Kiểm tra kết nối mạng
   - Restart client
   - Kiểm tra log file

6. **Java not found**
   - Cài đặt Java JDK từ Oracle hoặc OpenJDK
   - Thêm Java vào PATH environment variable

### Log files:
- Server: `notesync.log`
- Client: `notesync.log`

## Mở rộng

### Tính năng có thể thêm:
- **Authentication**: Đăng nhập người dùng
- **Encryption**: Mã hóa dữ liệu
- **File attachments**: Đính kèm file
- **Categories**: Phân loại ghi chú
- **Search**: Tìm kiếm nâng cao
- **Export**: Xuất ghi chú ra file
- **Database**: Lưu trữ persistent
- **Web interface**: Giao diện web
- **Mobile app**: Ứng dụng di động

### Cải tiến hiệu suất:
- **Connection pooling**: Pool kết nối
- **Caching**: Cache dữ liệu
- **Compression**: Nén dữ liệu
- **Load balancing**: Cân bằng tải
- **Clustering**: Server cluster

## Đóng góp

1. Fork project
2. Tạo feature branch
3. Commit changes
4. Push to branch
5. Tạo Pull Request

## License

MIT License - Xem file LICENSE để biết thêm chi tiết.

## Liên hệ

- Tác giả: [Tên của bạn]
- Email: [Email của bạn]
- Project: Đồ án môn Hệ Phân tán

---

**Lưu ý**: Đây là dự án học tập cho môn Hệ Phân tán. Code được thiết kế để minh họa các concepts của distributed systems như communication protocols, synchronization, và real-time updates.