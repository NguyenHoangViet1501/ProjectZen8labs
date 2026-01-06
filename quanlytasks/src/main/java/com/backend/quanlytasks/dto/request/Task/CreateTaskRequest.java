package com.backend.quanlytasks.dto.request.Task;

import com.backend.quanlytasks.common.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho request tạo mới Task
 */
@Data
public class CreateTaskRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Độ ưu tiên không được để trống")
    private Priority priority;

    private LocalDateTime dueDate;

    private List<String> tags;
}
