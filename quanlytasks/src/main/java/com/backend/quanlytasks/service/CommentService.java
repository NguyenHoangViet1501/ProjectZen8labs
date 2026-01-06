package com.backend.quanlytasks.service;

import com.backend.quanlytasks.dto.request.Comment.CreateCommentRequest;
import com.backend.quanlytasks.dto.response.Comment.CommentResponse;
import com.backend.quanlytasks.entity.User;

import java.util.List;

/**
 * Service interface cho các thao tác với Comment
 */
public interface CommentService {

    /**
     * Tạo comment mới
     */
    CommentResponse createComment(CreateCommentRequest request, User currentUser);

    /**
     * Lấy tất cả comments của một task
     */
    List<CommentResponse> getCommentsByTaskId(Long taskId);
}
