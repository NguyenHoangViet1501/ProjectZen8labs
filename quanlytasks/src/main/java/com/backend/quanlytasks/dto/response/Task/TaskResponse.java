package com.backend.quanlytasks.dto.response.Task;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO response cho thông tin task cơ bản
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {

    private Long id;

    private String title;

    private String description;

    private TaskStatus status;

    private Priority priority;

    private LocalDateTime dueDate;

    private Long createdById;

    private String createdByName;

    private Long assigneeId;

    private String assigneeName;

    private List<String> tags;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Flag để Admin biết task đã bị xóa hay chưa (cho toggle button)
     */
    private boolean isDeleted;
}
