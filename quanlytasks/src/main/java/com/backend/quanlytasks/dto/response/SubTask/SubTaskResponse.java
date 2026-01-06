package com.backend.quanlytasks.dto.response.SubTask;

import com.backend.quanlytasks.common.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO response cho thông tin SubTask
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubTaskResponse {

    private Long id;

    private String title;

    private String description;

    private TaskStatus status;

    private Long parentTaskId;

    private String parentTaskTitle;

    private Long assigneeId;

    private String assigneeName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
