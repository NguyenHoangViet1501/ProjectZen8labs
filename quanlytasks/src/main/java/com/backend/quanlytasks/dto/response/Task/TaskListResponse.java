package com.backend.quanlytasks.dto.response.Task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO response cho danh sách task với phân trang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskListResponse {

    private List<TaskResponse> tasks;

    private Integer currentPage;

    private Integer totalPages;

    private Long totalElements;

    private Integer pageSize;

    private Boolean hasNext;

    private Boolean hasPrevious;
}
