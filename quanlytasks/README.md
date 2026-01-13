# Hệ Thống Quản Lý Task (Project Zen8Labs)

Dự án phát triển hệ thống quản lý công việc (Task Management System) toàn diện, được xây dựng bằng **Java Spring Boot**, **Thymeleaf**, và **MySQL**. Hệ thống hỗ trợ làm việc nhóm hiệu quả với các tính năng phân quyền, thông báo thời gian thực, và theo dõi tiến độ chi tiết.

## 🚀 Tính Năng Nổi Bật

### 1. Quản Lý Công Việc (Tasks)
- **CRUD đầy đủ**: Tạo, Đọc, Cập nhật, Xóa công việc.
- **Thuộc tính phong phú**: Tiêu đề, Mô tả, Hạn chót (Deadline), Độ ưu tiên (Low, Medium, High), Tags (thẻ phân loại).
- **Trạng thái**: Todo, In Progress, Done, Cancelled.
- **Xóa mềm (Soft Delete)**: Task bị xóa sẽ vào thùng rác, Admin có thể khôi phục hoặc xóa vĩnh viễn và có thông báo xác nhận an toàn.

### 2. Quản Lý Subtask (Công việc phụ)
- Chia nhỏ công việc chính thành các đầu việc nhỏ hơn.
- Cập nhật trạng thái độc lập cho từng Subtask.
- Assign Subtask cho thành viên khác.

### 3. Phân Quyền & Bảo Mật (Auth)
- **Đăng ký/Đăng nhập**: Bảo mật với mã hóa mật khẩu BCrypt.
- **Phân quyền (RBAC)**:
  - **Admin**: Quản lý toàn bộ hệ thống, xóa/sửa mọi task, xem thống kê toàn cục, chuyển đổi trạng thái xóa/khôi phục.
  - **User**: Chỉ quản lý task do mình tạo hoặc được giao, xem thống kê cá nhân.
- **Bảo mật API**: Sử dụng JWT cho API và Session cho Web Interface.

### 4. Thông Báo & Tương Tác
- **Thông báo thời gian thực (Web Push)**: Tích hợp **Firebase Cloud Messaging (FCM)** để gửi thông báo đẩy ngay lập tức.
- **Hệ thống thông báo nội bộ**: Xem lại lịch sử thông báo, đánh dấu đã đọc.
- **Bình luận (Comments)**: Thảo luận, trao đổi ngay trên từng Task.

### 5. Tiện Ích Mở Rộng
- **Lịch sử hoạt động (Audit Log)**: Ghi lại mọi thay đổi (ai, làm gì, lúc nào) trên Task.
- **Xuất báo cáo**: Xuất danh sách Task ra file **Excel** (.xlsx).
- **Tìm kiếm & Lọc**: Tìm nhanh theo tiêu đề, trạng thái, độ ưu tiên.
- **Giao diện hiện đại**: Thiết kế Responsive với Bootstrap 5, Dark/Light mode toggle (tùy biến).

---

## 🛠️ Công Nghệ Sử Dụng

### Backend
- **Java 17+**
- **Spring Boot 3.x**: Framework chính.
- **Spring Security**: Xác thực & Phân quyền.
- **Spring Data JPA (Hibernate)**: ORM làm việc với Database.
- **Apache POI**: Xử lý xuất file Excel.

### Frontend
- **Thymeleaf**: Template Engine render giao diện server-side.
- **Bootstrap 5**: Framework CSS responsive.
- **JavaScript (ES6+)**: Xử lý logic phía client, gọi API, xử lý Modal.

### Database & Khác
- **MySQL**: Cơ sở dữ liệu quan hệ.
- **Firebase (FCM)**: Dịch vụ gửi thông báo.
- **Maven**: Quản lý phụ thuộc & Build tool.

---

## ⚙️ Cài Đặt & Chạy Dự Án

### Yêu Cầu
- JDK 17 trở lên.
- Maven.
- MySQL Server.

### Các Bước Cài Đặt

1.  **Clone dự án**
    ```bash
    git clone https://github.com/NguyenHoangViet1501/ProjectZen8labs.git
    cd quanlytasks
    ```

2.  **Cấu hình Database**
    - Mở file `src/main/resources/application.properties`.
    - Cập nhật username/password của MySQL:
      ```properties
      spring.datasource.url=jdbc:mysql://localhost:3306/quanlytasks?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      spring.datasource.username=root
      spring.datasource.password=your_password
      ```

3.  **Cấu hình Firebase (Cho thông báo)**
    - Đặt file `serviceAccountKey.json` (tải từ Firebase Console) vào thư mục `src/main/resources/`.

4.  **Chạy ứng dụng**
    ```bash
    mvn spring-boot:run
    ```
    Hoặc mở bằng IDE (IntelliJ/Eclipse/VSCode) và chạy class `QuanlytasksApplication`.

5.  **Truy cập**
    - Web Interface: `http://localhost:8080`
    - Swagger UI (nếu có cấu hình): `http://localhost:8080/swagger-ui.html`

---

## 📂 Cấu Trúc Dự Án

```
src/main/java/com/backend/quanlytasks
├── common/           # Enums, Constants
├── config/           # Security, Firebase, CORS config
├── controller/       # WebController (View) & Controllers (API)
├── dto/              # Data Transfer Objects (Request/Response)
├── entity/           # JPA Entities (Database Tables)
├── event/            # Event Listeners (Notification events)
├── repository/       # Data Access Layer
├── service/          # Business Logic Layer
└── QuanlytasksApplication.java
```

---

## 🤝 Đóng Góp
Dự án được phát triển bởi **NguyenHoangViet1501**. Mọi ý kiến đóng góp xin vui lòng tạo Pull Request hoặc Issue trên GitHub.
