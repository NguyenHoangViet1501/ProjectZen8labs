package com.backend.quanlytasks.dto.response.Comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO response cho thông tin Comment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long id;

    private String content;

    private Long taskId;

    private Long authorId;

    private String authorName;

    private LocalDateTime createdAt;
}
