# API Testing Guide - Task Management System

## Chuẩn bị trước khi test

### 1. Lấy JWT Token (Đăng nhập)
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
    "email": "admin@test.com",
    "password": "123456"
}
```

**Response:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> ⚠️ **Lưu ý**: Sử dụng token này cho header `Authorization: Bearer <token>` trong các request sau.

---

## API #3: Tạo Task
**Endpoint:** `POST /api/tasks`  
**Role:** ADMIN, USER

```http
POST http://localhost:8080/api/tasks
Authorization: Bearer <token>
Content-Type: application/json

{
    "title": "Hoàn thành module authentication",
    "description": "Cần hoàn thành đăng nhập, đăng ký và xác thực email",
    "priority": "HIGH",
    "dueDate": "2025-01-15T23:59:59",
    "tags": ["backend", "urgent", "authentication"]
}
```

---

## API #4: Cập nhật Task
**Endpoint:** `PUT /api/tasks/{id}`  
**Role:** ADMIN, USER (chỉ task của mình)

```http
PUT http://localhost:8080/api/tasks/1
Authorization: Bearer <token>
Content-Type: application/json

{
    "title": "Hoàn thành module authentication - Updated",
    "description": "Cần hoàn thành đăng nhập, đăng ký, xác thực email và reset password",
    "priority": "MEDIUM",
    "dueDate": "2025-01-20T23:59:59",
    "tags": ["backend", "authentication"]
}
```

---

## API #5: Xóa Task (Soft Delete)
**Endpoint:** `DELETE /api/tasks/{id}`  
**Role:** ADMIN, USER (chỉ task của mình)

```http
DELETE http://localhost:8080/api/tasks/1
Authorization: Bearer <token>
```

---

## API #6: Xem danh sách Task (Có filter và phân trang)
**Endpoint:** `GET /api/tasks`  
**Role:** ADMIN (xem tất cả), USER (xem task của mình)

### Lấy tất cả (không filter)
```http
GET http://localhost:8080/api/tasks
Authorization: Bearer <token>
```

### Với filter (đầy đủ tham số)
```http
GET http://localhost:8080/api/tasks?status=&priority=&assigneeId=&dueDateFrom=&dueDateTo=&page=0&size=8
Authorization: Bearer <token>
```

### Ví dụ filter cụ thể
```http
GET http://localhost:8080/api/tasks?status=TODO&priority=HIGH&assigneeId=2&dueDateFrom=2025-01-01T00:00:00&dueDateTo=2025-01-31T23:59:59&page=0&size=8
Authorization: Bearer <token>
```

### Tham số filter có thể dùng:
| Parameter | Type | Description | Default |
|-----------|------|-------------|---------|
| status | Enum | TODO, IN_PROGRESS, DONE, CANCELLED | (all) |
| priority | Enum | LOW, MEDIUM, HIGH | (all) |
| assigneeId | Long | ID của người được giao | (none) |
| dueDateFrom | DateTime | Từ ngày (ISO format) | (none) |
| dueDateTo | DateTime | Đến ngày (ISO format) | (none) |
| page | Integer | Số trang (bắt đầu từ 0) | 0 |
| size | Integer | Số item mỗi trang | **8** |

---

## API #7: Xem chi tiết Task (bao gồm subtasks, comments, history)
**Endpoint:** `GET /api/tasks/{id}`  
**Role:** ADMIN, USER

```http
GET http://localhost:8080/api/tasks/1
Authorization: Bearer <token>
```

---

## API #8: Assign Task (Chỉ ADMIN)
**Endpoint:** `PUT /api/tasks/{id}/assign`  
**Role:** ADMIN only

```http
PUT http://localhost:8080/api/tasks/1/assign
Authorization: Bearer <admin_token>
Content-Type: application/json

{
    "assigneeId": 2
}
```

---

## API #9: Cập nhật trạng thái Task
**Endpoint:** `PUT /api/tasks/{id}/status`  
**Role:** ADMIN, USER (người được assign)

```http
PUT http://localhost:8080/api/tasks/1/status
Authorization: Bearer <token>
Content-Type: application/json

{
    "status": "IN_PROGRESS"
}
```

**Các trạng thái:**
- `TODO` - Chưa làm
- `IN_PROGRESS` - Đang thực hiện
- `DONE` - Hoàn thành
- `CANCELLED` - Đã hủy

---

## API #10: Tạo SubTask
**Endpoint:** `POST /api/subtasks`  
**Role:** ADMIN, USER

```http
POST http://localhost:8080/api/subtasks
Authorization: Bearer <token>
Content-Type: application/json

{
    "parentTaskId": 1,
    "title": "Implement login API",
    "description": "Tạo endpoint POST /api/auth/login",
    "assigneeId": 2
}
```

---

## API #11: Cập nhật SubTask
**Endpoint:** `PUT /api/subtasks/{id}`  
**Role:** ADMIN, USER (người được assign)

```http
PUT http://localhost:8080/api/subtasks/1
Authorization: Bearer <token>
Content-Type: application/json

{
    "title": "Implement login API - Updated",
    "description": "Tạo endpoint POST /api/auth/login với JWT",
    "status": "IN_PROGRESS",
    "assigneeId": 2
}
```

---

## API #12: Xóa SubTask (Soft Delete)
**Endpoint:** `DELETE /api/subtasks/{id}`  
**Role:** ADMIN, USER (owner của task cha)

```http
DELETE http://localhost:8080/api/subtasks/1
Authorization: Bearer <token>
```

---

## API #13: Xem chi tiết SubTask
**Endpoint:** `GET /api/subtasks/{id}`  
**Role:** ADMIN, USER

```http
GET http://localhost:8080/api/subtasks/1
Authorization: Bearer <token>
```

---

## API #14: Comment trong Task
**Endpoint:** `POST /api/comments`  
**Role:** ADMIN, USER

```http
POST http://localhost:8080/api/comments
Authorization: Bearer <token>
Content-Type: application/json

{
    "taskId": 1,
    "content": "Đã review code, cần sửa một số chỗ về validation"
}
```

### Lấy comments của một task
```http
GET http://localhost:8080/api/comments/task/1
Authorization: Bearer <token>
```

> 💡 **Lưu ý**: Comments được sắp xếp theo thời gian tăng dần (comment cũ nhất hiển thị trước, mới nhất ở cuối).

---

## API #15-16: Notification

### Xem danh sách Notification
**Endpoint:** `GET /api/notifications`  
**Role:** ADMIN, USER

```http
GET http://localhost:8080/api/notifications?page=0&size=10
Authorization: Bearer <token>
```

### Đánh dấu đã đọc
```http
PUT http://localhost:8080/api/notifications/1/read
Authorization: Bearer <token>
```

### Đánh dấu tất cả đã đọc
```http
PUT http://localhost:8080/api/notifications/read-all
Authorization: Bearer <token>
```

### Cập nhật FCM Token (cho Push Notification)
**Endpoint:** `POST /api/notifications/fcm-token`  
**Role:** ADMIN, USER

Frontend gọi API này sau khi lấy được FCM token từ Firebase để nhận push notification.

```http
POST http://localhost:8080/api/notifications/fcm-token
Authorization: Bearer <token>
Content-Type: application/json

{
    "fcmToken": "eKz7dM9xR..."
}
```

**Response:**
```json
"Cập nhật FCM token thành công"
```

> 💡 **Lưu ý**: FCM token lấy từ Firebase JS SDK trên frontend. Khi có token mới (ví dụ: user đăng nhập trên thiết bị khác), cần gọi lại API này.

---

## API #17: Xem lịch sử Task
**Endpoint:** `GET /api/tasks/{id}/history`  
**Role:** ADMIN, USER

```http
GET http://localhost:8080/api/tasks/1/history
Authorization: Bearer <token>
```

**Response example:**
```json
[
    {
        "id": 1,
        "taskId": 1,
        "changedById": 1,
        "changedByName": "Admin User",
        "fieldName": "status",
        "oldValue": "TODO",
        "newValue": "IN_PROGRESS",
        "changedAt": "2025-01-02T10:30:00"
    },
    {
        "id": 2,
        "taskId": 1,
        "changedById": 1,
        "changedByName": "Admin User",
        "fieldName": "assignee",
        "oldValue": "Chưa giao",
        "newValue": "John Doe",
        "changedAt": "2025-01-02T10:25:00"
    }
]
```

---

## API #18: Xuất Report Task (Excel)
**Endpoint:** `GET /api/tasks/export`  
**Role:** ADMIN (xuất tất cả), USER (xuất task của mình)

```http
GET http://localhost:8080/api/tasks/export
Authorization: Bearer <token>
```

**Response:**
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- File: `tasks_report_YYYYMMDD.xlsx`

**Các cột trong file Excel:**
| # | Tên cột | Mô tả |
|---|---------|-------|
| 1 | ID | ID của task |
| 2 | Tiêu đề | Tên task |
| 3 | Mô tả | Mô tả chi tiết |
| 4 | Trạng thái | TODO, IN_PROGRESS, DONE, CANCELLED |
| 5 | Độ ưu tiên | LOW, MEDIUM, HIGH |
| 6 | Người được giao | Tên người được assign |
| 7 | Ngày deadline | Deadline của task |
| 8 | Ngày tạo | Thời gian tạo task |

---

## Bảng tổng hợp API Permissions

| # | Chức năng | Method | Endpoint | ADMIN | USER |
|---|-----------|--------|----------|-------|------|
| 3 | Tạo task | POST | /api/tasks | ✅ | ✅ |
| 4 | Cập nhật task | PUT | /api/tasks/{id} | ✅ | ✅ (owner/assignee) |
| 5 | Xóa task | DELETE | /api/tasks/{id} | ✅ | ✅ (owner) |
| 6 | Xem danh sách task | GET | /api/tasks | ✅ (all) | ✅ (own) |
| 7 | Xem chi tiết task | GET | /api/tasks/{id} | ✅ | ✅ (owner/assignee) |
| 8 | Assign task | PUT | /api/tasks/{id}/assign | ✅ | ❌ |
| 9 | Update status | PUT | /api/tasks/{id}/status | ✅ | ✅ (assignee) |
| 10 | Tạo subtask | POST | /api/subtasks | ✅ | ✅ |
| 11 | Update subtask | PUT | /api/subtasks/{id} | ✅ | ✅ (assignee) |
| 12 | Xóa subtask | DELETE | /api/subtasks/{id} | ✅ | ✅ (owner) |
| 13 | Xem chi tiết subtask | GET | /api/subtasks/{id} | ✅ | ✅ |
| 14 | Comment task | POST | /api/comments | ✅ | ✅ |
| 16 | Xem notification | GET | /api/notifications | ✅ | ✅ |
| 17 | Xem lịch sử task | GET | /api/tasks/{id}/history | ✅ | ✅ |
| 18 | Xuất report Excel | GET | /api/tasks/export | ✅ (all) | ✅ (own) |
| 19 | Cập nhật FCM Token | POST | /api/notifications/fcm-token | ✅ | ✅ |

---

## Validation Messages

| Field | Validation Message |
|-------|-------------------|
| title | "Tiêu đề không được để trống" |
| priority | "Độ ưu tiên không được để trống" |
| status | "Trạng thái không được để trống" |
| taskId | "ID task không được để trống" |
| content | "Nội dung comment không được để trống" |
| assigneeId | "ID người được giao không được để trống" |
| parentTaskId | "ID task cha không được để trống" |

---

## Ghi chú về Thông báo (Notifications)

### Ai nhận được thông báo?

| Hành động | Người nhận thông báo |
|-----------|---------------------|
| **Cập nhật Task** | Người tạo task, người thực hiện task, **người được giao subtask** |
| **Thay đổi trạng thái Task** | Người tạo task, người thực hiện task, **người được giao subtask** |
| **Giao Task cho người mới** | Người thực hiện mới, **người thực hiện cũ** (nếu có) |
| **Comment trong Task** | Người tạo task, người thực hiện task, **tất cả người được giao subtask** |
| **Thay đổi SubTask** | Người tạo task cha, người thực hiện task cha, người được giao subtask |

> 💡 **Lưu ý**: Người thực hiện hành động sẽ **không** nhận thông báo về hành động của chính mình.

