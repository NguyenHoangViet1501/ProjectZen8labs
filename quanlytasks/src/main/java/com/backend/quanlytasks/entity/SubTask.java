package com.backend.quanlytasks.entity;

import com.backend.quanlytasks.common.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho SubTask (công việc con) thuộc về một Task cha
 */
@Entity
@Table(name = "subtasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubTask {

    /**
     * ID duy nhất của subtask
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tiêu đề/tên của subtask
     * Bắt buộc phải có
     */
    @Column(nullable = false)
    private String title;

    /**
     * Mô tả chi tiết về subtask
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Trạng thái hiện tại của subtask
     * Các giá trị: TODO, IN_PROGRESS, DONE, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    /**
     * Task cha mà subtask này thuộc về
     * Quan hệ nhiều-một với Task
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id", nullable = false)
    private Task parentTask;

    /**
     * Người được giao thực hiện subtask
     * Có thể null nếu chưa được assign
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /**
     * Cờ xóa mềm (soft delete)
     * 0 = subtask đang hoạt động (active)
     * 1 = subtask đã bị xóa (deleted)
     */
    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Integer isDelete = 0;

    /**
     * Thời gian tạo subtask
     * Tự động set khi tạo mới
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật subtask lần cuối
     * Tự động cập nhật khi có thay đổi
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Tự động set thời gian tạo trước khi persist
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Tự động cập nhật thời gian khi update
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
