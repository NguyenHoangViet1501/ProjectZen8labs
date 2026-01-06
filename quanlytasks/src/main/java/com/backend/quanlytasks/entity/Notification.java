package com.backend.quanlytasks.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho Notification (thông báo) gửi cho người dùng
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /**
     * ID duy nhất của thông báo
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Người nhận thông báo
     * Quan hệ nhiều-một với User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /**
     * Tiêu đề của thông báo
     */
    @Column(nullable = false)
    private String title;

    /**
     * Nội dung chi tiết của thông báo
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * Task liên quan đến thông báo (nếu có)
     * Có thể null nếu thông báo không liên quan đến task cụ thể
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_task_id")
    private Task relatedTask;

    /**
     * Trạng thái đã đọc của thông báo
     * false = chưa đọc, true = đã đọc
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * Thời gian tạo thông báo
     * Tự động set khi tạo mới
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Tự động set thời gian tạo trước khi persist
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
