package com.backend.quanlytasks.service;

import com.backend.quanlytasks.dto.request.SubTask.CreateSubTaskRequest;
import com.backend.quanlytasks.dto.request.SubTask.UpdateSubTaskRequest;
import com.backend.quanlytasks.dto.response.SubTask.SubTaskResponse;
import com.backend.quanlytasks.entity.User;

import java.util.List;

/**
 * Service interface cho các thao tác với SubTask
 */
public interface SubTaskService {

    /**
     * Tạo subtask mới
     */
    SubTaskResponse createSubTask(CreateSubTaskRequest request, User currentUser);

    /**
     * Cập nhật subtask
     */
    SubTaskResponse updateSubTask(Long id, UpdateSubTaskRequest request, User currentUser, boolean isAdmin);

    /**
     * Xóa mềm subtask
     */
    void softDeleteSubTask(Long id, User currentUser, boolean isAdmin);

    /**
     * Lấy chi tiết subtask
     */
    SubTaskResponse getSubTaskDetail(Long id);

    /**
     * Lấy tất cả subtasks của một task
     */
    List<SubTaskResponse> getSubTasksByTaskId(Long taskId);
}
