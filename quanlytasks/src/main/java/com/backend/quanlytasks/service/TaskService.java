package com.backend.quanlytasks.service;

import com.backend.quanlytasks.dto.request.Task.*;
import com.backend.quanlytasks.dto.response.Task.TaskDetailResponse;
import com.backend.quanlytasks.dto.response.Task.TaskListResponse;
import com.backend.quanlytasks.dto.response.Task.TaskResponse;
import com.backend.quanlytasks.entity.User;

/**
 * Service interface cho các thao tác với Task
 */
public interface TaskService {

    /**
     * Tạo task mới
     */
    TaskResponse createTask(CreateTaskRequest request, User currentUser);

    /**
     * Cập nhật thông tin task
     */
    TaskResponse updateTask(Long id, UpdateTaskRequest request, User currentUser, boolean isAdmin);

    /**
     * Xóa mềm task
     */
    void softDeleteTask(Long id, User currentUser, boolean isAdmin);

    /**
     * Lấy danh sách task (có filter và phân trang)
     */
    TaskListResponse getTaskList(TaskFilterRequest filter, User currentUser, boolean isAdmin);

    /**
     * Lấy chi tiết task (bao gồm subtasks, comments, history)
     */
    TaskDetailResponse getTaskDetail(Long id, User currentUser, boolean isAdmin);

    /**
     * Giao task cho user (chỉ ADMIN)
     */
    TaskResponse assignTask(Long id, AssignTaskRequest request, User currentUser);

    /**
     * Cập nhật trạng thái task
     */
    TaskResponse updateTaskStatus(Long id, UpdateTaskStatusRequest request, User currentUser, boolean isAdmin);

    /**
     * Xuất danh sách task ra file Excel
     * ADMIN xuất tất cả task, USER xuất task của mình
     */
    byte[] exportTasksToExcel(User currentUser, boolean isAdmin);

    /**
     * Khôi phục task đã xóa (chỉ ADMIN)
     * Gửi notification cho assignee rằng task được hoàn tác
     */
    void restoreTask(Long id, User currentUser);
}
