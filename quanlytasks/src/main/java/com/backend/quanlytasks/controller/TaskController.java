package com.backend.quanlytasks.controller;

import com.backend.quanlytasks.common.enums.RoleName;
import com.backend.quanlytasks.dto.request.Task.*;
import com.backend.quanlytasks.dto.response.Task.TaskDetailResponse;
import com.backend.quanlytasks.dto.response.Task.TaskListResponse;
import com.backend.quanlytasks.dto.response.Task.TaskResponse;
import com.backend.quanlytasks.dto.response.TaskHistory.TaskHistoryResponse;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.TaskHistoryService;
import com.backend.quanlytasks.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các API liên quan đến Task
 * APIs: 3, 4, 5, 6, 7, 8, 9, 17
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskHistoryService taskHistoryService;
    private final UserRepository userRepository;

    /**
     * API #3: Tạo task mới
     * ADMIN và USER đều có thể tạo
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TaskResponse> createTask(
            @RequestBody @Valid CreateTaskRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        TaskResponse response = taskService.createTask(request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * API #4: Cập nhật task
     * ADMIN và USER đều có thể cập nhật (USER chỉ được cập nhật task của mình)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @RequestBody @Valid UpdateTaskRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        TaskResponse response = taskService.updateTask(id, request, currentUser, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * API #5: Xóa task (soft delete)
     * ADMIN và USER đều có thể xóa (USER chỉ được xóa task của mình)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<String> deleteTask(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        taskService.softDeleteTask(id, currentUser, isAdmin);
        return ResponseEntity.ok("Xóa task thành công");
    }

    /**
     * API #6: Xem danh sách task (có filter và phân trang)
     * ADMIN xem tất cả, USER chỉ xem task của mình
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TaskListResponse> getTaskList(
            @ModelAttribute TaskFilterRequest filter,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        TaskListResponse response = taskService.getTaskList(filter, currentUser, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * API #7: Xem chi tiết task (bao gồm subtasks, comments, history)
     * ADMIN và USER đều có thể xem (USER chỉ xem được task của mình)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TaskDetailResponse> getTaskDetail(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        TaskDetailResponse response = taskService.getTaskDetail(id, currentUser, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * API #8: Assign task cho user
     * Chỉ ADMIN mới có quyền
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable Long id,
            @RequestBody @Valid AssignTaskRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        TaskResponse response = taskService.assignTask(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * API #9: Cập nhật trạng thái task
     * ADMIN và USER đều có thể (USER chỉ được cập nhật task được assign cho mình)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @RequestBody @Valid UpdateTaskStatusRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        TaskResponse response = taskService.updateTaskStatus(id, request, currentUser, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * API #17: Xem lịch sử task
     * ADMIN và USER đều có thể xem
     */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<TaskHistoryResponse>> getTaskHistory(
            @PathVariable Long id) {

        List<TaskHistoryResponse> history = taskHistoryService.getTaskHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * API #18: Xuất danh sách task ra file Excel
     * ADMIN xuất tất cả task, USER xuất task của mình
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<byte[]> exportTasksToExcel(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        byte[] excelData = taskService.exportTasksToExcel(currentUser, isAdmin);

        String filename = "tasks_report_" + java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelData.length);

        return new ResponseEntity<>(excelData, headers, org.springframework.http.HttpStatus.OK);
    }

    /**
     * Helper: Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }

    /**
     * Helper: Kiểm tra user có phải ADMIN không
     */
    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
