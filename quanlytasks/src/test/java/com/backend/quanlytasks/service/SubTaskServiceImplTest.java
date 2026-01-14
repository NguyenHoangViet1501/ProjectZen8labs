package com.backend.quanlytasks.service;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.TaskStatus;
import com.backend.quanlytasks.dto.request.SubTask.CreateSubTaskRequest;
import com.backend.quanlytasks.dto.request.SubTask.UpdateSubTaskRequest;
import com.backend.quanlytasks.dto.response.SubTask.SubTaskResponse;
import com.backend.quanlytasks.entity.SubTask;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.SubTaskRepository;
import com.backend.quanlytasks.repository.TaskRepository;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.Impl.SubTaskServiceImpl;
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
class SubTaskServiceImplTest {

    @Mock
    private SubTaskRepository subTaskRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TaskHistoryService taskHistoryService;

    @InjectMocks
    private SubTaskServiceImpl subTaskService;

    private User creator;
    private User assignee;
    private Task parentTask;
    private SubTask subTask;
    private CreateSubTaskRequest createRequest;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setEmail("creator@test.com");
        creator.setFullName("Creator User");

        assignee = new User();
        assignee.setId(2L);
        assignee.setEmail("assignee@test.com");
        assignee.setFullName("Assignee User");

        parentTask = Task.builder()
                .id(1L)
                .title("Parent Task")
                .description("Parent Description")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .createdBy(creator)
                .tags(new HashSet<>())
                .isDelete(0)
                .build();

        subTask = SubTask.builder()
                .id(1L)
                .title("SubTask 1")
                .description("SubTask Description")
                .status(TaskStatus.TODO)
                .parentTask(parentTask)
                .assignee(assignee)
                .isDelete(0)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = new CreateSubTaskRequest();
        createRequest.setParentTaskId(1L);
        createRequest.setTitle("New SubTask");
        createRequest.setDescription("New SubTask Description");
        createRequest.setAssigneeId(2L);
    }

    @Test
    @DisplayName("Create SubTask - Success by Task Creator")
    void createSubTask_Success() {
        // Arrange
        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(parentTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(subTaskRepository.save(any(SubTask.class))).thenAnswer(invocation -> {
            SubTask saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        SubTaskResponse response = subTaskService.createSubTask(createRequest, creator, false);

        // Assert
        assertNotNull(response);
        assertEquals("New SubTask", response.getTitle());
        verify(taskHistoryService).logChange(eq(parentTask), eq(creator), eq("subtask_created"), isNull(), anyString());
    }

    @Test
    @DisplayName("Create SubTask - Success by Admin")
    void createSubTask_ByAdmin_Success() {
        // Arrange
        User admin = new User();
        admin.setId(3L);
        admin.setFullName("Admin User");

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(parentTask));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(subTaskRepository.save(any(SubTask.class))).thenAnswer(invocation -> {
            SubTask saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        SubTaskResponse response = subTaskService.createSubTask(createRequest, admin, true);

        // Assert
        assertNotNull(response);
    }

    @Test
    @DisplayName("Create SubTask - Not Admin Not Creator - Throws Exception")
    void createSubTask_NotAdminNotCreator_ThrowsException() {
        // Arrange
        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(parentTask));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> subTaskService.createSubTask(createRequest, assignee, false));
        assertEquals("Chỉ Admin hoặc người tạo task mới có quyền tạo subtask", exception.getMessage());
    }

    @Test
    @DisplayName("Create SubTask - Parent Task Not Found - Throws Exception")
    void createSubTask_ParentNotFound_ThrowsException() {
        // Arrange
        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> subTaskService.createSubTask(createRequest, creator, false));
        assertEquals("Không tìm thấy task cha", exception.getMessage());
    }

    @Test
    @DisplayName("Update SubTask - Success by Task Creator")
    void updateSubTask_Success() {
        // Arrange
        UpdateSubTaskRequest updateRequest = new UpdateSubTaskRequest();
        updateRequest.setTitle("Updated SubTask");
        updateRequest.setStatus(TaskStatus.IN_PROGRESS);

        when(subTaskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(subTask));
        when(subTaskRepository.save(any(SubTask.class))).thenReturn(subTask);

        // Act
        SubTaskResponse response = subTaskService.updateSubTask(1L, updateRequest, creator, false);

        // Assert
        assertNotNull(response);
        verify(taskHistoryService, atLeastOnce()).logChange(eq(parentTask), eq(creator), anyString(), anyString(),
                anyString());
    }

    @Test
    @DisplayName("Update SubTask - Success by Assignee")
    void updateSubTask_ByAssignee_Success() {
        // Arrange
        UpdateSubTaskRequest updateRequest = new UpdateSubTaskRequest();
        updateRequest.setStatus(TaskStatus.DONE);

        when(subTaskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(subTask));
        when(subTaskRepository.save(any(SubTask.class))).thenReturn(subTask);

        // Act
        SubTaskResponse response = subTaskService.updateSubTask(1L, updateRequest, assignee, false);

        // Assert
        assertNotNull(response);
    }

    @Test
    @DisplayName("Update SubTask - No Permission - Throws Exception")
    void updateSubTask_NoPermission_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(99L);

        UpdateSubTaskRequest updateRequest = new UpdateSubTaskRequest();
        updateRequest.setTitle("Updated");

        when(subTaskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(subTask));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> subTaskService.updateSubTask(1L, updateRequest, otherUser, false));
        assertEquals("Không có quyền cập nhật subtask này", exception.getMessage());
    }

    @Test
    @DisplayName("Soft Delete SubTask - Success")
    void softDeleteSubTask_Success() {
        // Arrange
        when(subTaskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(subTask));
        when(subTaskRepository.save(any(SubTask.class))).thenReturn(subTask);

        // Act & Assert
        assertDoesNotThrow(() -> subTaskService.softDeleteSubTask(1L, creator, false));
        verify(subTaskRepository).save(any(SubTask.class));
    }

    @Test
    @DisplayName("Soft Delete SubTask - Not Owner - Throws Exception")
    void softDeleteSubTask_NotOwner_ThrowsException() {
        // Arrange
        when(subTaskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(subTask));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> subTaskService.softDeleteSubTask(1L, assignee, false));
        assertEquals("Không có quyền xóa subtask này", exception.getMessage());
    }

    @Test
    @DisplayName("Get SubTask Detail - Success")
    void getSubTaskDetail_Success() {
        // Arrange
        when(subTaskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(subTask));

        // Act
        SubTaskResponse response = subTaskService.getSubTaskDetail(1L);

        // Assert
        assertNotNull(response);
        assertEquals("SubTask 1", response.getTitle());
    }

    @Test
    @DisplayName("Get SubTask Detail - Not Found - Throws Exception")
    void getSubTaskDetail_NotFound_ThrowsException() {
        // Arrange
        when(subTaskRepository.findByIdAndIsDelete(99L, 0)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> subTaskService.getSubTaskDetail(99L));
        assertEquals("Không tìm thấy subtask", exception.getMessage());
    }

    @Test
    @DisplayName("Get SubTasks By TaskId - Success")
    void getSubTasksByTaskId_Success() {
        // Arrange
        when(subTaskRepository.findByParentTaskIdAndIsDelete(1L, 0)).thenReturn(List.of(subTask));

        // Act
        List<SubTaskResponse> responses = subTaskService.getSubTasksByTaskId(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("SubTask 1", responses.get(0).getTitle());
    }

    @Test
    @DisplayName("Get SubTasks By TaskId - Empty List")
    void getSubTasksByTaskId_EmptyList() {
        // Arrange
        when(subTaskRepository.findByParentTaskIdAndIsDelete(1L, 0)).thenReturn(List.of());

        // Act
        List<SubTaskResponse> responses = subTaskService.getSubTasksByTaskId(1L);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }
}
