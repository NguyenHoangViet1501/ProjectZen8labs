package com.backend.quanlytasks.controller;

import com.backend.quanlytasks.dto.response.Notification.NotificationListResponse;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * API liên quan đến Notification
 * APIs: 15, 16
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * API #16: Xem danh sách notification
     * ADMIN và USER đều có thể xem (chỉ xem notification của mình)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<NotificationListResponse> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        NotificationListResponse response = notificationService.getNotifications(currentUser, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Đánh dấu một notification đã đọc
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok("Đánh dấu đã đọc thành công");
    }

    /**
     * Đánh dấu tất cả notification đã đọc
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<String> markAllAsRead(
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok("Đánh dấu tất cả đã đọc thành công");
    }

    /**
     * Helper: Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }
}
