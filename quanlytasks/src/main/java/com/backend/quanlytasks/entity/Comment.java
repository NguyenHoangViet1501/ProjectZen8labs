package com.backend.quanlytasks.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity đại diện cho Comment (bình luận) trong một Task
 */
@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    /**
     * ID duy nhất của comment
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nội dung của comment
     * Bắt buộc phải có và có thể chứa nội dung dài
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Task mà comment này thuộc về
     * Quan hệ nhiều-một với Task
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * Người viết comment
     * Quan hệ nhiều-một với User
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * Thời gian tạo comment
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
