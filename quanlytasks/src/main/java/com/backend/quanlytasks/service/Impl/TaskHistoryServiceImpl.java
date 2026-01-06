package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.dto.response.TaskHistory.TaskHistoryResponse;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.TaskHistory;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.TaskHistoryRepository;
import com.backend.quanlytasks.service.TaskHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskHistoryServiceImpl implements TaskHistoryService {

    private final TaskHistoryRepository taskHistoryRepository;

    @Override
    public void logChange(Task task, User changedBy, String fieldName, String oldValue, String newValue) {
        TaskHistory history = TaskHistory.builder()
                .task(task)
                .changedBy(changedBy)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        taskHistoryRepository.save(history);
    }

    @Override
    public List<TaskHistoryResponse> getTaskHistory(Long taskId) {
        List<TaskHistory> histories = taskHistoryRepository.findByTaskIdOrderByChangedAtDesc(taskId);
        return histories.stream()
                .map(this::mapToTaskHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mapper: TaskHistory -> TaskHistoryResponse
     */
    private TaskHistoryResponse mapToTaskHistoryResponse(TaskHistory history) {
        return TaskHistoryResponse.builder()
                .id(history.getId())
                .taskId(history.getTask().getId())
                .changedById(history.getChangedBy().getId())
                .changedByName(history.getChangedBy().getFullName())
                .fieldName(history.getFieldName())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .changedAt(history.getChangedAt())
                .build();
    }
}
