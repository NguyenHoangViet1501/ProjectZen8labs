package com.backend.quanlytasks.dto.request.Task;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO cho request giao task cho user
 */
@Data
public class AssignTaskRequest {

    @NotNull(message = "ID người được giao không được để trống")
    private Long assigneeId;
}
