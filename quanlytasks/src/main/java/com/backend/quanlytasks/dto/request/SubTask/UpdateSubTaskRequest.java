package com.backend.quanlytasks.dto.request.SubTask;

import com.backend.quanlytasks.common.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO cho request cập nhật SubTask
 */
@Data
public class UpdateSubTaskRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    private TaskStatus status;

    private Long assigneeId;
}
