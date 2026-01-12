package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.dto.response.Notification.NotificationListResponse;
import com.backend.quanlytasks.dto.response.Notification.NotificationResponse;
import com.backend.quanlytasks.entity.Notification;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.event.TaskNotificationEvent;
import com.backend.quanlytasks.event.TaskNotificationEvent.NotificationType;
import com.backend.quanlytasks.repository.NotificationRepository;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Legacy method - giữ để backward compatible
     * Publish event với type mặc định là TASK_UPDATED
     */
    @Override
    public void sendNotification(User recipient, String title, String message, Task relatedTask) {
        publishTaskNotification(recipient, title, message, relatedTask, NotificationType.TASK_UPDATED);
    }

    /**
     * Publish TaskNotificationEvent
     * Event sẽ được xử lý bởi TaskNotificationEventListener
     */
    @Override
    public void publishTaskNotification(User recipient, String title, String message,
            Task relatedTask, NotificationType type) {
        log.info("Publishing TaskNotificationEvent: {} - {} cho user: {}",
                type, title, recipient.getEmail());

        TaskNotificationEvent event = new TaskNotificationEvent(
                this, recipient, title, message, relatedTask, type);

        eventPublisher.publishEvent(event);
    }

    @Override
    public NotificationListResponse getNotifications(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable);

        long unreadCount = notificationRepository.countByRecipientIdAndIsRead(user.getId(), false);

        return NotificationListResponse.builder()
                .notifications(notificationPage.getContent().stream()
                        .map(this::mapToNotificationResponse)
                        .collect(Collectors.toList()))
                .unreadCount(unreadCount)
                .currentPage(notificationPage.getNumber())
                .totalPages(notificationPage.getTotalPages())
                .totalElements(notificationPage.getTotalElements())
                .hasNext(notificationPage.hasNext())
                .hasPrevious(notificationPage.hasPrevious())
                .build();
    }

    @Override
    public void markAsRead(Long notificationId, User currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Không có quyền truy cập thông báo này");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(User currentUser) {
        notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId())
                .forEach(notification -> {
                    notification.setIsRead(true);
                    notificationRepository.save(notification);
                });
    }

    @Override
    @Transactional
    public void updateFcmToken(User user, String fcmToken) {
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        log.info("Đã cập nhật FCM token cho user: {}", user.getEmail());
    }

    /**
     * Mapper: Notification -> NotificationResponse
     */
    private NotificationResponse mapToNotificationResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedTaskId(notification.getRelatedTask() != null ? notification.getRelatedTask().getId() : null)
                .relatedTaskTitle(
                        notification.getRelatedTask() != null ? notification.getRelatedTask().getTitle() : null)
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
