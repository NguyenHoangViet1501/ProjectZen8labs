package com.backend.quanlytasks.event;

import com.backend.quanlytasks.entity.Notification;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.NotificationRepository;
import com.backend.quanlytasks.service.FirebaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Event Listener lắng nghe TaskNotificationEvent
 * Xử lý: lưu notification vào DB và gửi FCM push notification
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskNotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final FirebaseService firebaseService;

    /**
     * Xử lý event thông báo task
     * Chạy async để không block main thread
     */
    @EventListener
    @Async
    public void handleTaskNotificationEvent(TaskNotificationEvent event) {
        log.info("Nhận được TaskNotificationEvent: {} - {}", event.getType(), event.getTitle());

        User recipient = event.getRecipient();

        // 1. Lưu notification vào database
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(event.getTitle())
                .message(event.getMessage())
                .relatedTask(event.getRelatedTask())
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Đã lưu notification vào database cho user: {}", recipient.getEmail());

        // 2. Gửi FCM push notification nếu user có token
        if (recipient.getFcmToken() != null && !recipient.getFcmToken().isEmpty()) {
            Map<String, String> data = new HashMap<>();
            data.put("type", event.getType().name());

            if (event.getRelatedTask() != null) {
                data.put("taskId", event.getRelatedTask().getId().toString());
                data.put("taskTitle", event.getRelatedTask().getTitle());
            }

            boolean sent = firebaseService.sendPushNotification(
                    recipient.getFcmToken(),
                    event.getTitle(),
                    event.getMessage(),
                    data);

            if (sent) {
                log.info("Đã gửi FCM push notification cho user: {}", recipient.getEmail());
            } else {
                log.warn("Không thể gửi FCM push notification cho user: {}", recipient.getEmail());
            }
        } else {
            log.info("User {} không có FCM token, bỏ qua push notification", recipient.getEmail());
        }
    }
}
