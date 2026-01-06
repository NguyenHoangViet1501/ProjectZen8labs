package com.backend.quanlytasks.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho lịch sử thay đổi của Task
 * Ghi lại mọi thay đổi: ai thay đổi, khi nào, trường nào, giá trị cũ và mới
 */
@Entity
@Table(name = "task_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskHistory {

    /**
     * ID duy nhất của bản ghi lịch sử
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Task được ghi lịch sử
     * Quan hệ nhiều-một với Task
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * Người thực hiện thay đổi
     * Quan hệ nhiều-một với User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    /**
     * Tên trường bị thay đổi
     * Ví dụ: "title", "status", "priority", "assignee", "description"
     */
    @Column(name = "field_name", nullable = false)
    private String fieldName;

    /**
     * Giá trị cũ trước khi thay đổi
     * Có thể null nếu là lần đầu set giá trị
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * Giá trị mới sau khi thay đổi
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Thời gian thực hiện thay đổi
     * Tự động set khi tạo mới
     */
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /**
     * Tự động set thời gian thay đổi trước khi persist
     */
    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
