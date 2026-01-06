package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.dto.response.Notification.NotificationListResponse;
import com.backend.quanlytasks.dto.response.Notification.NotificationResponse;
import com.backend.quanlytasks.entity.Notification;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.NotificationRepository;
import com.backend.quanlytasks.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void sendNotification(User recipient, String title, String message, Task relatedTask) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .title(title)
                .message(message)
                .relatedTask(relatedTask)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
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
