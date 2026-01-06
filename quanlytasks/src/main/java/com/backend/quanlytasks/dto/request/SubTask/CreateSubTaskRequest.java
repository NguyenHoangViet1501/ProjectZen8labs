package com.backend.quanlytasks.dto.request.SubTask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO cho request tạo mới SubTask
 */
@Data
public class CreateSubTaskRequest {

    @NotNull(message = "ID task cha không được để trống")
    private Long parentTaskId;

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    private Long assigneeId;
}
