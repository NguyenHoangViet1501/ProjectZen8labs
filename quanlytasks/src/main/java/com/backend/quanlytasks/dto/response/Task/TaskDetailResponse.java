package com.backend.quanlytasks.dto.response.Task;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.TaskStatus;
import com.backend.quanlytasks.dto.response.Comment.CommentResponse;
import com.backend.quanlytasks.dto.response.SubTask.SubTaskResponse;
import com.backend.quanlytasks.dto.response.TaskHistory.TaskHistoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO response cho chi tiết task (bao gồm subtasks, comments, history)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDetailResponse {

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

    private List<SubTaskResponse> subtasks;

    private List<CommentResponse> comments;

    private List<TaskHistoryResponse> history;
}
