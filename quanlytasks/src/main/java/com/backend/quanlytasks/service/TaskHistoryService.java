package com.backend.quanlytasks.service;

import com.backend.quanlytasks.dto.response.TaskHistory.TaskHistoryResponse;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;

import java.util.List;

/**
 * Service interface cho ghi và truy vấn lịch sử thay đổi Task
 */
public interface TaskHistoryService {

    /**
     * Ghi log thay đổi của task
     */
    void logChange(Task task, User changedBy, String fieldName, String oldValue, String newValue);

    /**
     * Lấy lịch sử thay đổi của task
     */
    List<TaskHistoryResponse> getTaskHistory(Long taskId);
}
