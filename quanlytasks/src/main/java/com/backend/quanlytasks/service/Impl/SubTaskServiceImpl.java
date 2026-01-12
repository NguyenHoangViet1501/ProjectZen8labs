package com.backend.quanlytasks.service.Impl;

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
import com.backend.quanlytasks.service.SubTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubTaskServiceImpl implements SubTaskService {

    private final SubTaskRepository subTaskRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    public SubTaskResponse createSubTask(CreateSubTaskRequest request, User currentUser, boolean isAdmin) {
        Task parentTask = taskRepository.findByIdAndIsDelete(request.getParentTaskId(), 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task cha"));

        // Check permission: only Admin or Task Creator can create subtask
        if (!isAdmin && !parentTask.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Chỉ Admin hoặc người tạo task mới có quyền tạo subtask");
        }

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người được giao"));
        }

        SubTask subTask = mapToSubTask(request, parentTask, assignee);
        subTask = subTaskRepository.save(subTask);

        return mapToSubTaskResponse(subTask);
    }

    @Override
    public SubTaskResponse updateSubTask(Long id, UpdateSubTaskRequest request, User currentUser, boolean isAdmin) {
        SubTask subTask = subTaskRepository.findByIdAndIsDelete(id, 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy subtask"));

        // Check permission: ADMIN or assignee
        if (!isAdmin && subTask.getAssignee() != null
                && !subTask.getAssignee().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Không có quyền cập nhật subtask này");
        }

        // Update fields only if provided (not null)
        if (request.getTitle() != null) {
            subTask.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            subTask.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            subTask.setStatus(request.getStatus());
        }

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người được giao"));
            subTask.setAssignee(assignee);
        }

        subTask = subTaskRepository.save(subTask);
        return mapToSubTaskResponse(subTask);
    }

    @Override
    public void softDeleteSubTask(Long id, User currentUser, boolean isAdmin) {
        SubTask subTask = subTaskRepository.findByIdAndIsDelete(id, 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy subtask"));

        // Check permission: ADMIN or parent task owner
        if (!isAdmin && !subTask.getParentTask().getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Không có quyền xóa subtask này");
        }

        subTask.setIsDelete(1);
        subTaskRepository.save(subTask);
    }

    @Override
    public SubTaskResponse getSubTaskDetail(Long id) {
        SubTask subTask = subTaskRepository.findByIdAndIsDelete(id, 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy subtask"));

        return mapToSubTaskResponse(subTask);
    }

    @Override
    public List<SubTaskResponse> getSubTasksByTaskId(Long taskId) {
        List<SubTask> subTasks = subTaskRepository.findByParentTaskIdAndIsDelete(taskId, 0);
        return subTasks.stream()
                .map(this::mapToSubTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mapper: CreateSubTaskRequest -> SubTask
     */
    private SubTask mapToSubTask(CreateSubTaskRequest request, Task parentTask, User assignee) {
        return SubTask.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .parentTask(parentTask)
                .assignee(assignee)
                .isDelete(0)
                .build();
    }

    /**
     * Mapper: SubTask -> SubTaskResponse
     */
    private SubTaskResponse mapToSubTaskResponse(SubTask subTask) {
        return SubTaskResponse.builder()
                .id(subTask.getId())
                .title(subTask.getTitle())
                .description(subTask.getDescription())
                .status(subTask.getStatus())
                .parentTaskId(subTask.getParentTask().getId())
                .parentTaskTitle(subTask.getParentTask().getTitle())
                .assigneeId(subTask.getAssignee() != null ? subTask.getAssignee().getId() : null)
                .assigneeName(subTask.getAssignee() != null ? subTask.getAssignee().getFullName() : null)
                .createdAt(subTask.getCreatedAt())
                .updatedAt(subTask.getUpdatedAt())
                .build();
    }
}
