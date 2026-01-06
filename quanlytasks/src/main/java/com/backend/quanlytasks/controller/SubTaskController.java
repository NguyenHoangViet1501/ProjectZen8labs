package com.backend.quanlytasks.controller;

import com.backend.quanlytasks.dto.request.SubTask.CreateSubTaskRequest;
import com.backend.quanlytasks.dto.request.SubTask.UpdateSubTaskRequest;
import com.backend.quanlytasks.dto.response.SubTask.SubTaskResponse;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.SubTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

/**
 * API liên quan đến SubTask
 * APIs: 10, 11, 12, 13
 */
@RestController
@RequestMapping("/api/subtasks")
@RequiredArgsConstructor
public class SubTaskController {

    private final SubTaskService subTaskService;
    private final UserRepository userRepository;

    /**
     * API #10: Tạo subtask
     * ADMIN và USER đều có thể tạo
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SubTaskResponse> createSubTask(
            @RequestBody @Valid CreateSubTaskRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        SubTaskResponse response = subTaskService.createSubTask(request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * API #11: Cập nhật subtask
     * ADMIN và USER đều có thể (USER chỉ được cập nhật subtask được assign cho
     * mình)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SubTaskResponse> updateSubTask(
            @PathVariable Long id,
            @RequestBody @Valid UpdateSubTaskRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        SubTaskResponse response = subTaskService.updateSubTask(id, request, currentUser, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * API #12: Xóa subtask (soft delete)
     * ADMIN và USER đều có thể (USER chỉ được xóa subtask của task do mình tạo)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<String> deleteSubTask(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);
        subTaskService.softDeleteSubTask(id, currentUser, isAdmin);
        return ResponseEntity.ok("Xóa subtask thành công");
    }

    /**
     * API #13: Xem chi tiết subtask
     * ADMIN và USER đều có thể xem
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SubTaskResponse> getSubTaskDetail(
            @PathVariable Long id) {

        SubTaskResponse response = subTaskService.getSubTaskDetail(id);
        return ResponseEntity.ok(response);
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
