package com.backend.quanlytasks.service;

import com.backend.quanlytasks.dto.response.Notification.NotificationListResponse;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;

/**
 * Service interface cho các thao tác với Notification
 */
public interface NotificationService {

    /**
     * Gửi thông báo cho user
     */
    void sendNotification(User recipient, String title, String message, Task relatedTask);

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
}
