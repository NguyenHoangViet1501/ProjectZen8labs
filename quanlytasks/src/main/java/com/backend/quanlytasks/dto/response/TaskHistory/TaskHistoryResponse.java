package com.backend.quanlytasks.dto.response.TaskHistory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO response cho lịch sử thay đổi Task
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskHistoryResponse {

    private Long id;

    private Long taskId;

    private Long changedById;

    private String changedByName;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private LocalDateTime changedAt;
}
