package com.backend.quanlytasks.dto.request.Task;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho request filter/lọc danh sách task
 */
@Data
public class TaskFilterRequest {

    private TaskStatus status;

    private LocalDateTime dueDateFrom;

    private LocalDateTime dueDateTo;

    private List<String> tags;

    private Priority priority;

    private Long assigneeId;

    private Integer page = 0;

    private Integer size = 8;
}
