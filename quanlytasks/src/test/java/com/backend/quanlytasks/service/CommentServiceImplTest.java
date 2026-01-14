package com.backend.quanlytasks.service;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.TaskStatus;
import com.backend.quanlytasks.dto.request.Comment.CreateCommentRequest;
import com.backend.quanlytasks.dto.response.Comment.CommentResponse;
import com.backend.quanlytasks.entity.Comment;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.CommentRepository;
import com.backend.quanlytasks.repository.TaskRepository;
import com.backend.quanlytasks.service.Impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User creator;
    private User commenter;
    private User assignee;
    private Task task;
    private Comment comment;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setEmail("creator@test.com");
        creator.setFullName("Creator User");

        commenter = new User();
        commenter.setId(2L);
        commenter.setEmail("commenter@test.com");
        commenter.setFullName("Commenter User");

        assignee = new User();
        assignee.setId(3L);
        assignee.setEmail("assignee@test.com");
        assignee.setFullName("Assignee User");

        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .createdBy(creator)
                .assignee(assignee)
                .tags(new HashSet<>())
                .isDelete(0)
                .build();

        comment = Comment.builder()
                .id(1L)
                .content("Test comment")
                .task(task)
                .author(commenter)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Create Comment - Success")
    void createComment_Success() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest();
        request.setTaskId(1L);
        request.setContent("New comment");

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        CommentResponse response = commentService.createComment(request, commenter);

        // Assert
        assertNotNull(response);
        assertEquals("New comment", response.getContent());
        assertEquals(commenter.getId(), response.getAuthorId());

        // Verify notifications sent to creator and assignee
        verify(notificationService).publishTaskNotification(eq(creator), anyString(), anyString(), eq(task), any());
        verify(notificationService).publishTaskNotification(eq(assignee), anyString(), anyString(), eq(task), any());
    }

    @Test
    @DisplayName("Create Comment - By Creator - No Self Notification")
    void createComment_ByCreator_NoSelfNotification() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest();
        request.setTaskId(1L);
        request.setContent("Creator's comment");

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        CommentResponse response = commentService.createComment(request, creator);

        // Assert
        assertNotNull(response);
        // Notification should only go to assignee, not to creator (since creator is
        // commenting)
        verify(notificationService, times(1)).publishTaskNotification(eq(assignee), anyString(), anyString(), eq(task),
                any());
        verify(notificationService, never()).publishTaskNotification(eq(creator), anyString(), anyString(), any(),
                any());
    }

    @Test
    @DisplayName("Create Comment - Task Not Found - Throws Exception")
    void createComment_TaskNotFound_ThrowsException() {
        // Arrange
        CreateCommentRequest request = new CreateCommentRequest();
        request.setTaskId(99L);
        request.setContent("Comment");

        when(taskRepository.findByIdAndIsDelete(99L, 0)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> commentService.createComment(request, commenter));
        assertEquals("Không tìm thấy task", exception.getMessage());
    }

    @Test
    @DisplayName("Create Comment - Task With No Assignee")
    void createComment_NoAssignee() {
        // Arrange
        task.setAssignee(null);
        CreateCommentRequest request = new CreateCommentRequest();
        request.setTaskId(1L);
        request.setContent("Comment on unassigned task");

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        CommentResponse response = commentService.createComment(request, commenter);

        // Assert
        assertNotNull(response);
        // Only creator should be notified
        verify(notificationService, times(1)).publishTaskNotification(eq(creator), anyString(), anyString(), eq(task),
                any());
    }

    @Test
    @DisplayName("Get Comments By TaskId - Success")
    void getCommentsByTaskId_Success() {
        // Arrange
        Comment comment2 = Comment.builder()
                .id(2L)
                .content("Another comment")
                .task(task)
                .author(creator)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        when(commentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(comment, comment2));

        // Act
        List<CommentResponse> responses = commentService.getCommentsByTaskId(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Test comment", responses.get(0).getContent());
        assertEquals("Another comment", responses.get(1).getContent());
    }

    @Test
    @DisplayName("Get Comments By TaskId - Empty List")
    void getCommentsByTaskId_EmptyList() {
        // Arrange
        when(commentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        // Act
        List<CommentResponse> responses = commentService.getCommentsByTaskId(1L);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("Get Comments By TaskId - Verify Response Mapping")
    void getCommentsByTaskId_VerifyMapping() {
        // Arrange
        when(commentRepository.findByTaskIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(comment));

        // Act
        List<CommentResponse> responses = commentService.getCommentsByTaskId(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());

        CommentResponse response = responses.get(0);
        assertEquals(comment.getId(), response.getId());
        assertEquals(comment.getContent(), response.getContent());
        assertEquals(comment.getTask().getId(), response.getTaskId());
        assertEquals(comment.getAuthor().getId(), response.getAuthorId());
        assertEquals(comment.getAuthor().getFullName(), response.getAuthorName());
    }
}
