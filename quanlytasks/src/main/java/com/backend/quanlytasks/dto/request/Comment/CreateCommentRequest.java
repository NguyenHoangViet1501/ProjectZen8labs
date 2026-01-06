package com.backend.quanlytasks.dto.request.Comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO cho request tạo comment
 */
@Data
public class CreateCommentRequest {

    @NotNull(message = "ID task không được để trống")
    private Long taskId;

    @NotBlank(message = "Nội dung comment không được để trống")
    private String content;
}
