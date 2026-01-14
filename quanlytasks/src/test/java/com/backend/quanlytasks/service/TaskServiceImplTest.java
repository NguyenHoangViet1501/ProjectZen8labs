package com.backend.quanlytasks.service;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.RoleName;
import com.backend.quanlytasks.common.enums.TaskStatus;
import com.backend.quanlytasks.dto.request.Task.*;
import com.backend.quanlytasks.dto.response.Task.TaskDetailResponse;
import com.backend.quanlytasks.dto.response.Task.TaskListResponse;
import com.backend.quanlytasks.dto.response.Task.TaskResponse;
import com.backend.quanlytasks.entity.Role;
import com.backend.quanlytasks.entity.Tag;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.TagRepository;
import com.backend.quanlytasks.repository.TaskRepository;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.Impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskHistoryService taskHistoryService;

    @Mock
    private SubTaskService subTaskService;

    @Mock
    private CommentService commentService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User creator;
    private User assignee;
    private User admin;
    private Task task;
    private CreateTaskRequest createRequest;
    private UpdateTaskRequest updateRequest;

    @BeforeEach
    void setUp() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleName.USER);

        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(RoleName.ADMIN);

        creator = new User();
        creator.setId(1L);
        creator.setEmail("creator@test.com");
        creator.setFullName("Creator User");
        creator.setRoles(Set.of(userRole));

        assignee = new User();
        assignee.setId(2L);
        assignee.setEmail("assignee@test.com");
        assignee.setFullName("Assignee User");
        assignee.setRoles(Set.of(userRole));

        admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@test.com");
        admin.setFullName("Admin User");
        admin.setRoles(Set.of(adminRole));

        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(7))
                .createdBy(creator)
                .assignee(null)
                .tags(new HashSet<>())
                .isDelete(0)
                .build();

        createRequest = new CreateTaskRequest();
        createRequest.setTitle("New Task");
        createRequest.setDescription("New Description");
        createRequest.setPriority(Priority.HIGH);
        createRequest.setDueDate(LocalDateTime.now().plusDays(5));
        createRequest.setTags(List.of("urgent", "backend"));

        updateRequest = new UpdateTaskRequest();
        updateRequest.setTitle("Updated Task");
        updateRequest.setDescription("Updated Description");
        updateRequest.setPriority(Priority.LOW);
        updateRequest.setDueDate(LocalDateTime.now().plusDays(10));
    }

    @Test
    @DisplayName("Create Task - Success")
    void createTask_Success() {
        // Arrange
        when(tagRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setId(1L);
            return tag;
        });
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            savedTask.setId(1L);
            savedTask.setCreatedAt(LocalDateTime.now());
            return savedTask;
        });

        // Act
        TaskResponse response = taskService.createTask(createRequest, creator);

        // Assert
        assertNotNull(response);
        assertEquals("New Task", response.getTitle());
        assertEquals(TaskStatus.TODO, response.getStatus());
        verify(taskHistoryService).logChange(any(Task.class), eq(creator), eq("created"), isNull(), anyString());
    }

    @Test
    @DisplayName("Create Task - Past Due Date - Throws Exception")
    void createTask_PastDueDate_ThrowsException() {
        // Arrange
        createRequest.setDueDate(LocalDateTime.now().minusDays(1));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.createTask(createRequest, creator));
        assertEquals("Deadline không được là thời gian trong quá khứ", exception.getMessage());
    }

    @Test
    @DisplayName("Update Task - Success by Creator")
    void updateTask_Success() {
        // Arrange
        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        TaskResponse response = taskService.updateTask(1L, updateRequest, creator, false);

        // Assert
        assertNotNull(response);
        verify(taskHistoryService, atLeastOnce()).logChange(any(Task.class), eq(creator), anyString(), anyString(),
                anyString());
    }

    @Test
    @DisplayName("Update Task - Not Creator Not Admin - Throws Exception")
    void updateTask_NotCreator_ThrowsException() {
        // Arrange
        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.updateTask(1L, updateRequest, assignee, false));
        assertEquals("Chỉ ADMIN hoặc người tạo task mới có quyền cập nhật thông tin task", exception.getMessage());
    }

    @Test
    @DisplayName("Update Task - Admin Can Update Any Task")
    void updateTask_AdminSuccess() {
        // Arrange
        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        TaskResponse response = taskService.updateTask(1L, updateRequest, admin, true);

        // Assert
        assertNotNull(response);
    }

    @Test
    @DisplayName("Soft Delete Task - Success by Creator")
    void softDeleteTask_Success() {
        // Arrange
        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(userRepository.findByRolesName(RoleName.ADMIN)).thenReturn(List.of(admin));

        // Act & Assert
        assertDoesNotThrow(() -> taskService.softDeleteTask(1L, creator, false));
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Soft Delete Task - Not Owner Not Admin - Throws Exception")
    void softDeleteTask_NotOwnerNotAdmin_ThrowsException() {
        // Arrange
        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.softDeleteTask(1L, assignee, false));
        assertEquals("Chỉ người tạo hoặc Admin mới có quyền xóa task", exception.getMessage());
    }

    @Test
    @DisplayName("Assign Task - Success")
    void assignTask_Success() {
        // Arrange
        AssignTaskRequest request = new AssignTaskRequest();
        request.setAssigneeId(2L);

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        TaskResponse response = taskService.assignTask(1L, request, creator);

        // Assert
        assertNotNull(response);
        verify(notificationService).publishTaskNotification(eq(assignee), anyString(), anyString(), any(Task.class),
                any());
    }

    @Test
    @DisplayName("Assign Task - Not Creator - Throws Exception")
    void assignTask_NotCreator_ThrowsException() {
        // Arrange
        AssignTaskRequest request = new AssignTaskRequest();
        request.setAssigneeId(2L);

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.assignTask(1L, request, assignee));
        assertEquals("Bạn không có quyền giao task này. Chỉ người tạo task mới được giao.", exception.getMessage());
    }

    @Test
    @DisplayName("Update Task Status - Success by Creator")
    void updateTaskStatus_Success() {
        // Arrange
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        TaskResponse response = taskService.updateTaskStatus(1L, request, creator, false);

        // Assert
        assertNotNull(response);
        verify(taskHistoryService).logChange(any(Task.class), eq(creator), eq("status"), anyString(), anyString());
    }

    @Test
    @DisplayName("Update Task Status - Success by Assignee")
    void updateTaskStatus_ByAssignee_Success() {
        // Arrange
        task.setAssignee(assignee);
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus(TaskStatus.DONE);

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act
        TaskResponse response = taskService.updateTaskStatus(1L, request, assignee, false);

        // Assert
        assertNotNull(response);
    }

    @Test
    @DisplayName("Update Task Status - No Permission - Throws Exception")
    void updateTaskStatus_NoPermission_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(99L);

        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.updateTaskStatus(1L, request, otherUser, false));
        assertEquals("Bạn không có quyền cập nhật trạng thái task này", exception.getMessage());
    }

    @Test
    @DisplayName("Get Task List - Admin Sees All Tasks")
    void getTaskList_AdminSeesAll() {
        // Arrange
        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setPage(0);
        filter.setSize(10);

        Page<Task> taskPage = new PageImpl<>(List.of(task));
        when(taskRepository.findAllWithFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(taskPage);

        // Act
        TaskListResponse response = taskService.getTaskList(filter, admin, true);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTasks().size());
    }

    @Test
    @DisplayName("Get Task List - User Sees Own Tasks")
    void getTaskList_UserSeesOwn() {
        // Arrange
        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setPage(0);
        filter.setSize(10);

        Page<Task> taskPage = new PageImpl<>(List.of(task));
        when(taskRepository.findByUserWithFilters(eq(creator.getId()), any(), any(), any(), any(), any(),
                any(Pageable.class)))
                .thenReturn(taskPage);

        // Act
        TaskListResponse response = taskService.getTaskList(filter, creator, false);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTasks().size());
    }

    @Test
    @DisplayName("Get Task Detail - Success")
    void getTaskDetail_Success() {
        // Arrange
        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(subTaskService.getSubTasksByTaskId(1L)).thenReturn(List.of());
        when(commentService.getCommentsByTaskId(1L)).thenReturn(List.of());
        when(taskHistoryService.getTaskHistory(1L)).thenReturn(List.of());

        // Act
        TaskDetailResponse response = taskService.getTaskDetail(1L, creator, false);

        // Assert
        assertNotNull(response);
        assertEquals("Test Task", response.getTitle());
    }

    @Test
    @DisplayName("Get Task Detail - No Permission - Throws Exception")
    void getTaskDetail_NoPermission_ThrowsException() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(99L);

        when(taskRepository.findByIdAndIsDelete(1L, 0)).thenReturn(Optional.of(task));
        when(subTaskService.getSubTasksByTaskId(1L)).thenReturn(List.of());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getTaskDetail(1L, otherUser, false));
        assertEquals("Không có quyền xem task này", exception.getMessage());
    }

    @Test
    @DisplayName("Restore Task - Success")
    void restoreTask_Success() {
        // Arrange
        task.setIsDelete(1);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Act & Assert
        assertDoesNotThrow(() -> taskService.restoreTask(1L, admin));
        verify(taskHistoryService).logChange(any(Task.class), eq(admin), eq("restored"), anyString(), anyString());
    }

    @Test
    @DisplayName("Restore Task - Not Deleted - Throws Exception")
    void restoreTask_NotDeleted_ThrowsException() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.restoreTask(1L, admin));
        assertEquals("Task này chưa bị xóa", exception.getMessage());
    }
}
