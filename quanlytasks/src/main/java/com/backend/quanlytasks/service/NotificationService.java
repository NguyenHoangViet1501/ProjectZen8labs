package com.backend.quanlytasks.service;

import com.backend.quanlytasks.dto.response.Notification.NotificationListResponse;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.event.TaskNotificationEvent.NotificationType;

/**
 * Service interface cho các thao tác với Notification
 */
public interface NotificationService {

    /**
     * Gửi thông báo cho user (legacy method - vẫn giữ để backward compatible)
     * Sử dụng publishTaskNotification để có đầy đủ tính năng
     */
    void sendNotification(User recipient, String title, String message, Task relatedTask);

    /**
     * Publish event thông báo task (sử dụng Spring Application Events)
     * Event sẽ được xử lý async: lưu DB và gửi FCM push notification
     * 
     * @param recipient   Người nhận thông báo
     * @param title       Tiêu đề thông báo
     * @param message     Nội dung thông báo
     * @param relatedTask Task liên quan
     * @param type        Loại thông báo (TASK_ASSIGNED, TASK_STATUS_CHANGED,
     *                    TASK_UPDATED)
     */
    void publishTaskNotification(User recipient, String title, String message,
            Task relatedTask, NotificationType type);

    /**
     * Lấy danh sách thông báo của user
     */
    NotificationListResponse getNotifications(User user, int page, int size);

    /**
     * Đánh dấu thông báo đã đọc
     */
    void markAsRead(Long notificationId, User currentUser);

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    void markAllAsRead(User currentUser);

    /**
     * Cập nhật FCM token cho user
     * 
     * @param user     User cần cập nhật
     * @param fcmToken FCM token từ Firebase
     */
    void updateFcmToken(User user, String fcmToken);
}
