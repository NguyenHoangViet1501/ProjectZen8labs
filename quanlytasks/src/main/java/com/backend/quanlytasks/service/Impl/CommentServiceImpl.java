package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.dto.request.Comment.CreateCommentRequest;
import com.backend.quanlytasks.dto.response.Comment.CommentResponse;
import com.backend.quanlytasks.entity.Comment;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.CommentRepository;
import com.backend.quanlytasks.repository.TaskRepository;
import com.backend.quanlytasks.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;

    @Override
    public CommentResponse createComment(CreateCommentRequest request, User currentUser) {
        Task task = taskRepository.findByIdAndIsDelete(request.getTaskId(), 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

        Comment comment = mapToComment(request, task, currentUser);
        comment = commentRepository.save(comment);

        return mapToCommentResponse(comment);
    }

    @Override
    public List<CommentResponse> getCommentsByTaskId(Long taskId) {
        List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        return comments.stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mapper: CreateCommentRequest -> Comment
     */
    private Comment mapToComment(CreateCommentRequest request, Task task, User author) {
        return Comment.builder()
                .content(request.getContent())
                .task(task)
                .author(author)
                .build();
    }

    /**
     * Mapper: Comment -> CommentResponse
     */
    private CommentResponse mapToCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .taskId(comment.getTask().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getFullName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
