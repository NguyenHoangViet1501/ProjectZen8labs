package com.backend.quanlytasks.service;

import com.backend.quanlytasks.dto.response.Notification.NotificationListResponse;
import com.backend.quanlytasks.entity.Notification;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.event.TaskNotificationEvent;
import com.backend.quanlytasks.event.TaskNotificationEvent.NotificationType;
import com.backend.quanlytasks.repository.NotificationRepository;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.Impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User recipient;
    private Task relatedTask;
    private Notification notification;

    @BeforeEach
    void setUp() {
        recipient = new User();
        recipient.setId(1L);
        recipient.setEmail("recipient@test.com");
        recipient.setFullName("Recipient User");

        User creator = new User();
        creator.setId(2L);
        creator.setFullName("Creator");

        relatedTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .createdBy(creator)
                .build();

        notification = Notification.builder()
                .id(1L)
                .title("Test Notification")
                .message("Test Message")
                .recipient(recipient)
                .relatedTask(relatedTask)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Publish Task Notification - Success")
    void publishTaskNotification_Success() {
        // Act
        notificationService.publishTaskNotification(
                recipient,
                "Test Title",
                "Test Message",
                relatedTask,
                NotificationType.TASK_ASSIGNED);

        // Assert
        ArgumentCaptor<TaskNotificationEvent> eventCaptor = ArgumentCaptor.forClass(TaskNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        TaskNotificationEvent capturedEvent = eventCaptor.getValue();
        assertEquals(recipient, capturedEvent.getRecipient());
        assertEquals("Test Title", capturedEvent.getTitle());
        assertEquals("Test Message", capturedEvent.getMessage());
        assertEquals(NotificationType.TASK_ASSIGNED, capturedEvent.getType());
    }

    @Test
    @DisplayName("Send Notification (Legacy) - Uses Default Type")
    void sendNotification_UsesDefaultType() {
        // Act
        notificationService.sendNotification(recipient, "Title", "Message", relatedTask);

        // Assert
        ArgumentCaptor<TaskNotificationEvent> eventCaptor = ArgumentCaptor.forClass(TaskNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        TaskNotificationEvent capturedEvent = eventCaptor.getValue();
        assertEquals(NotificationType.TASK_UPDATED, capturedEvent.getType());
    }

    @Test
    @DisplayName("Get Notifications - Success with Pagination")
    void getNotifications_Success() {
        // Arrange
        Page<Notification> notificationPage = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(notificationRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(1L);

        // Act
        NotificationListResponse response = notificationService.getNotifications(recipient, 0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getNotifications().size());
        assertEquals(1L, response.getUnreadCount());
        assertEquals(0, response.getCurrentPage());
    }

    @Test
    @DisplayName("Get Notifications - Empty List")
    void getNotifications_EmptyList() {
        // Arrange
        Page<Notification> emptyPage = new PageImpl<>(List.of());
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(notificationRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(0L);

        // Act
        NotificationListResponse response = notificationService.getNotifications(recipient, 0, 10);

        // Assert
        assertNotNull(response);
        assertTrue(response.getNotifications().isEmpty());
        assertEquals(0L, response.getUnreadCount());
    }

    @Test
    @DisplayName("Mark As Read - Success")
    void markAsRead_Success() {
        // Arrange
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act & Assert
        assertDoesNotThrow(() -> notificationService.markAsRead(1L, recipient));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Mark As Read - Not Found - Throws Exception")
    void markAsRead_NotFound_ThrowsException() {
        // Arrange
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead(99L, recipient));
        assertEquals("Không tìm thấy thông báo", exception.getMessage());
    }

    @Test
    @DisplayName("Mark As Read - Not Owner - Throws Exception")
    void markAsRead_NotOwner_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(99L);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead(1L, otherUser));
        assertEquals("Không có quyền truy cập thông báo này", exception.getMessage());
    }

    @Test
    @DisplayName("Mark All As Read - Success")
    void markAllAsRead_Success() {
        // Arrange
        Notification notification2 = Notification.builder()
                .id(2L)
                .title("Second Notification")
                .recipient(recipient)
                .isRead(false)
                .build();

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification, notification2));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        notificationService.markAllAsRead(recipient);

        // Assert
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Update FCM Token - Success")
    void updateFcmToken_Success() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(recipient);

        // Act
        notificationService.updateFcmToken(recipient, "new-fcm-token");

        // Assert
        assertEquals("new-fcm-token", recipient.getFcmToken());
        verify(userRepository).save(recipient);
    }

    @Test
    @DisplayName("Get Notifications - Verify Response Mapping")
    void getNotifications_VerifyMapping() {
        // Arrange
        Page<Notification> notificationPage = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(notificationPage);
        when(notificationRepository.countByRecipientIdAndIsRead(1L, false)).thenReturn(1L);

        // Act
        NotificationListResponse response = notificationService.getNotifications(recipient, 0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getNotifications().size());

        var notifResponse = response.getNotifications().get(0);
        assertEquals(notification.getId(), notifResponse.getId());
        assertEquals(notification.getTitle(), notifResponse.getTitle());
        assertEquals(notification.getMessage(), notifResponse.getMessage());
        assertEquals(notification.getRelatedTask().getId(), notifResponse.getRelatedTaskId());
        assertEquals(notification.getIsRead(), notifResponse.getIsRead());
    }
}
