package com.backend.quanlytasks.service;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.TaskStatus;
import com.backend.quanlytasks.dto.response.TaskHistory.TaskHistoryResponse;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.TaskHistory;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.TaskHistoryRepository;
import com.backend.quanlytasks.service.Impl.TaskHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskHistoryServiceImplTest {

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @InjectMocks
    private TaskHistoryServiceImpl taskHistoryService;

    private User user;
    private Task task;
    private TaskHistory taskHistory;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setFullName("Test User");

        task = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .createdBy(user)
                .tags(new HashSet<>())
                .isDelete(0)
                .build();

        taskHistory = TaskHistory.builder()
                .id(1L)
                .task(task)
                .changedBy(user)
                .fieldName("status")
                .oldValue("TODO")
                .newValue("IN_PROGRESS")
                .changedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Log Change - Success")
    void logChange_Success() {
        // Arrange
        when(taskHistoryRepository.save(any(TaskHistory.class))).thenReturn(taskHistory);

        // Act
        taskHistoryService.logChange(task, user, "status", "TODO", "IN_PROGRESS");

        // Assert
        ArgumentCaptor<TaskHistory> historyCaptor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryRepository).save(historyCaptor.capture());

        TaskHistory captured = historyCaptor.getValue();
        assertEquals(task, captured.getTask());
        assertEquals(user, captured.getChangedBy());
        assertEquals("status", captured.getFieldName());
        assertEquals("TODO", captured.getOldValue());
        assertEquals("IN_PROGRESS", captured.getNewValue());
    }

    @Test
    @DisplayName("Log Change - With Null Old Value")
    void logChange_WithNullOldValue() {
        // Arrange
        when(taskHistoryRepository.save(any(TaskHistory.class))).thenReturn(taskHistory);

        // Act
        taskHistoryService.logChange(task, user, "created", null, "Task được tạo");

        // Assert
        ArgumentCaptor<TaskHistory> historyCaptor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryRepository).save(historyCaptor.capture());

        TaskHistory captured = historyCaptor.getValue();
        assertNull(captured.getOldValue());
        assertEquals("Task được tạo", captured.getNewValue());
    }

    @Test
    @DisplayName("Log Change - Title Update")
    void logChange_TitleUpdate() {
        // Arrange
        when(taskHistoryRepository.save(any(TaskHistory.class))).thenReturn(taskHistory);

        // Act
        taskHistoryService.logChange(task, user, "title", "Old Title", "New Title");

        // Assert
        verify(taskHistoryRepository).save(any(TaskHistory.class));
    }

    @Test
    @DisplayName("Get Task History - Success")
    void getTaskHistory_Success() {
        // Arrange
        TaskHistory history2 = TaskHistory.builder()
                .id(2L)
                .task(task)
                .changedBy(user)
                .fieldName("priority")
                .oldValue("MEDIUM")
                .newValue("HIGH")
                .changedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(1L))
                .thenReturn(List.of(taskHistory, history2));

        // Act
        List<TaskHistoryResponse> responses = taskHistoryService.getTaskHistory(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("status", responses.get(0).getFieldName());
        assertEquals("priority", responses.get(1).getFieldName());
    }

    @Test
    @DisplayName("Get Task History - Empty List")
    void getTaskHistory_EmptyList() {
        // Arrange
        when(taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(999L)).thenReturn(List.of());

        // Act
        List<TaskHistoryResponse> responses = taskHistoryService.getTaskHistory(999L);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("Get Task History - Verify Response Mapping")
    void getTaskHistory_VerifyMapping() {
        // Arrange
        when(taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(1L)).thenReturn(List.of(taskHistory));

        // Act
        List<TaskHistoryResponse> responses = taskHistoryService.getTaskHistory(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());

        TaskHistoryResponse response = responses.get(0);
        assertEquals(taskHistory.getId(), response.getId());
        assertEquals(taskHistory.getTask().getId(), response.getTaskId());
        assertEquals(taskHistory.getChangedBy().getId(), response.getChangedById());
        assertEquals(taskHistory.getChangedBy().getFullName(), response.getChangedByName());
        assertEquals(taskHistory.getFieldName(), response.getFieldName());
        assertEquals(taskHistory.getOldValue(), response.getOldValue());
        assertEquals(taskHistory.getNewValue(), response.getNewValue());
    }

    @Test
    @DisplayName("Log Change - Assignee Change")
    void logChange_AssigneeChange() {
        // Arrange
        when(taskHistoryRepository.save(any(TaskHistory.class))).thenReturn(taskHistory);

        // Act
        taskHistoryService.logChange(task, user, "assignee", "Chưa giao", "New Assignee");

        // Assert
        ArgumentCaptor<TaskHistory> historyCaptor = ArgumentCaptor.forClass(TaskHistory.class);
        verify(taskHistoryRepository).save(historyCaptor.capture());

        TaskHistory captured = historyCaptor.getValue();
        assertEquals("assignee", captured.getFieldName());
        assertEquals("Chưa giao", captured.getOldValue());
        assertEquals("New Assignee", captured.getNewValue());
    }

    @Test
    @DisplayName("Log Change - Delete Operation")
    void logChange_DeleteOperation() {
        // Arrange
        when(taskHistoryRepository.save(any(TaskHistory.class))).thenReturn(taskHistory);

        // Act
        taskHistoryService.logChange(task, user, "deleted", null, "Task đã bị xóa");

        // Assert
        verify(taskHistoryRepository).save(any(TaskHistory.class));
    }
}
