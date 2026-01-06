package com.backend.quanlytasks.controller;

import com.backend.quanlytasks.dto.request.Comment.CreateCommentRequest;
import com.backend.quanlytasks.dto.response.Comment.CommentResponse;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các API liên quan đến Comment
 * API: 14
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    /**
     * API #14: Tạo comment trong task
     * ADMIN và USER đều có thể tạo
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<CommentResponse> createComment(
            @RequestBody @Valid CreateCommentRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        CommentResponse response = commentService.createComment(request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách comments của một task
     */
    @GetMapping("/task/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<CommentResponse>> getCommentsByTaskId(
            @PathVariable Long taskId) {

        List<CommentResponse> comments = commentService.getCommentsByTaskId(taskId);
        return ResponseEntity.ok(comments);
    }

    /**
     * Helper: Lấy user hiện tại từ authentication
     */
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }
}
