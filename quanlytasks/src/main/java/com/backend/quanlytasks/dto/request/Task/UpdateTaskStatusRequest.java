package com.backend.quanlytasks.dto.request.Task;

import com.backend.quanlytasks.common.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO cho request cập nhật trạng thái task
 */
@Data
public class UpdateTaskStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private TaskStatus status;
}
