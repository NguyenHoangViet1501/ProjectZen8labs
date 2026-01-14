package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.dto.request.Comment.CreateCommentRequest;
import com.backend.quanlytasks.dto.response.Comment.CommentResponse;
import com.backend.quanlytasks.entity.Comment;
import com.backend.quanlytasks.entity.SubTask;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.event.TaskNotificationEvent.NotificationType;
import com.backend.quanlytasks.repository.CommentRepository;
import com.backend.quanlytasks.repository.SubTaskRepository;
import com.backend.quanlytasks.repository.TaskRepository;
import com.backend.quanlytasks.service.CommentService;
import com.backend.quanlytasks.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final NotificationService notificationService;

    @Override
    public CommentResponse createComment(CreateCommentRequest request, User currentUser) {
        Task task = taskRepository.findByIdAndIsDelete(request.getTaskId(), 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

        Comment comment = mapToComment(request, task, currentUser);
        comment = commentRepository.save(comment);

        // Collect users to notify (avoid duplicate notifications)
        Set<Long> notifiedUserIds = new HashSet<>();
        notifiedUserIds.add(currentUser.getId());

        // Gửi thông báo cho người tạo task (nếu không phải người comment)
        if (task.getCreatedBy() != null && !task.getCreatedBy().getId().equals(currentUser.getId())) {
            notifiedUserIds.add(task.getCreatedBy().getId());
            notificationService.publishTaskNotification(
                    task.getCreatedBy(),
                    "Comment mới trong task",
                    currentUser.getFullName() + " đã comment trong task \"" + task.getTitle() + "\"",
                    task,
                    NotificationType.TASK_COMMENTED);
        }

        // Gửi thông báo cho người được giao task (nếu khác người comment và người tạo)
        if (task.getAssignee() != null && !notifiedUserIds.contains(task.getAssignee().getId())) {
            notifiedUserIds.add(task.getAssignee().getId());
            notificationService.publishTaskNotification(
                    task.getAssignee(),
                    "Comment mới trong task",
                    currentUser.getFullName() + " đã comment trong task \"" + task.getTitle() + "\"",
                    task,
                    NotificationType.TASK_COMMENTED);
        }

        // Gửi thông báo cho tất cả người được giao subtask
        List<SubTask> subtasks = subTaskRepository.findByParentTaskIdAndIsDelete(task.getId(), 0);
        for (SubTask subTask : subtasks) {
            User subtaskAssignee = subTask.getAssignee();
            if (subtaskAssignee != null && !notifiedUserIds.contains(subtaskAssignee.getId())) {
                notifiedUserIds.add(subtaskAssignee.getId());
                notificationService.publishTaskNotification(
                        subtaskAssignee,
                        "Comment mới trong task",
                        currentUser.getFullName() + " đã comment trong task \"" + task.getTitle() + "\"",
                        task,
                        NotificationType.TASK_COMMENTED);
            }
        }

        return mapToCommentResponse(comment);
    }

    @Override
    public List<CommentResponse> getCommentsByTaskId(Long taskId) {
        List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
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
