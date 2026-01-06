package com.backend.quanlytasks.entity;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity đại diện cho Task (công việc) trong hệ thống quản lý task
 */
@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    /**
     * ID duy nhất của task
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tiêu đề/tên của task
     * Bắt buộc phải có
     */
    @Column(nullable = false)
    private String title;

    /**
     * Mô tả chi tiết về task
     * Có thể chứa nội dung dài
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Trạng thái hiện tại của task
     * Các giá trị: TODO, IN_PROGRESS, DONE, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    /**
     * Độ ưu tiên của task
     * Các giá trị: LOW, MEDIUM, HIGH
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    /**
     * Hạn hoàn thành của task
     * Có thể null nếu không có deadline
     */
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    /**
     * Người tạo task
     * Quan hệ nhiều-một với User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    /**
     * Người được giao thực hiện task
     * Có thể null nếu chưa được assign
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    /**
     * Danh sách các tag gắn với task
     * Quan hệ nhiều-nhiều với Tag
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    /**
     * Cờ xóa mềm (soft delete)
     * 0 = task đang hoạt động (active)
     * 1 = task đã bị xóa (deleted)
     */
    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Integer isDelete = 0;

    /**
     * Thời gian tạo task
     * Tự động set khi tạo mới
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật task lần cuối
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
