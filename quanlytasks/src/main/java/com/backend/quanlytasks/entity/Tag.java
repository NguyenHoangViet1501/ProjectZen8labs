package com.backend.quanlytasks.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity đại diện cho Tag (nhãn) để phân loại task
 */
@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    /**
     * ID duy nhất của tag
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên của tag, phải là duy nhất trong hệ thống
     * Ví dụ: "urgent", "backend", "frontend"
     */
    @Column(unique = true, nullable = false)
    private String name;
}
