package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.common.enums.TaskStatus;
import com.backend.quanlytasks.dto.request.SubTask.CreateSubTaskRequest;
import com.backend.quanlytasks.dto.request.SubTask.UpdateSubTaskRequest;
import com.backend.quanlytasks.dto.response.SubTask.SubTaskResponse;
import com.backend.quanlytasks.entity.SubTask;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.event.TaskNotificationEvent.NotificationType;
import com.backend.quanlytasks.repository.SubTaskRepository;
import com.backend.quanlytasks.repository.TaskRepository;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.NotificationService;
import com.backend.quanlytasks.service.SubTaskService;
import com.backend.quanlytasks.service.TaskHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubTaskServiceImpl implements SubTaskService {

    private final SubTaskRepository subTaskRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TaskHistoryService taskHistoryService;

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

        // Log history: Subtask created
        taskHistoryService.logChange(parentTask, currentUser,
                "subtask_created",
                null,
                "Subtask \"" + subTask.getTitle() + "\" được tạo" +
                        (assignee != null ? " (giao cho " + assignee.getFullName() + ")" : ""));

        // Send notifications for subtask creation
        sendSubTaskNotifications(
                subTask,
                parentTask,
                currentUser,
                "Subtask mới được tạo",
                "Subtask \"" + subTask.getTitle() + "\" đã được tạo trong task \"" + parentTask.getTitle() + "\"",
                NotificationType.TASK_UPDATED);

        return mapToSubTaskResponse(subTask);
    }

    @Override
    public SubTaskResponse updateSubTask(Long id, UpdateSubTaskRequest request, User currentUser, boolean isAdmin) {
        SubTask subTask = subTaskRepository.findByIdAndIsDelete(id, 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy subtask"));

        Task parentTask = subTask.getParentTask();

        // Check permission: ADMIN, parent task creator, or subtask assignee
        boolean isParentTaskCreator = parentTask.getCreatedBy().getId().equals(currentUser.getId());
        boolean isSubtaskAssignee = subTask.getAssignee() != null
                && subTask.getAssignee().getId().equals(currentUser.getId());

        if (!isAdmin && !isParentTaskCreator && !isSubtaskAssignee) {
            throw new RuntimeException("Không có quyền cập nhật subtask này");
        }

        // Track old values for history
        String oldTitle = subTask.getTitle();
        String oldDescription = subTask.getDescription();
        String oldStatus = subTask.getStatus() != null ? subTask.getStatus().name() : null;
        String oldAssigneeName = subTask.getAssignee() != null ? subTask.getAssignee().getFullName() : "Chưa giao";
        boolean statusChanged = false;

        // Update fields only if provided (not null) and log history
        if (request.getTitle() != null && !request.getTitle().equals(oldTitle)) {
            subTask.setTitle(request.getTitle());
            taskHistoryService.logChange(parentTask, currentUser,
                    "subtask_title",
                    "Subtask \"" + oldTitle + "\"",
                    "Subtask \"" + request.getTitle() + "\"");
        }

        if (request.getDescription() != null && !request.getDescription().equals(oldDescription)) {
            subTask.setDescription(request.getDescription());
            taskHistoryService.logChange(parentTask, currentUser,
                    "subtask_description",
                    "Subtask \"" + subTask.getTitle() + "\" mô tả cũ",
                    "Subtask \"" + subTask.getTitle() + "\" mô tả mới");
        }

        if (request.getStatus() != null && !request.getStatus().equals(subTask.getStatus())) {
            statusChanged = true;
            subTask.setStatus(request.getStatus());
            taskHistoryService.logChange(parentTask, currentUser,
                    "subtask_status",
                    "Subtask \"" + subTask.getTitle() + "\": " + oldStatus,
                    "Subtask \"" + subTask.getTitle() + "\": " + request.getStatus().name());
        }

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người được giao"));
            if (subTask.getAssignee() == null || !subTask.getAssignee().getId().equals(assignee.getId())) {
                taskHistoryService.logChange(parentTask, currentUser,
                        "subtask_assignee",
                        "Subtask \"" + subTask.getTitle() + "\": " + oldAssigneeName,
                        "Subtask \"" + subTask.getTitle() + "\": " + assignee.getFullName());
            }
            subTask.setAssignee(assignee);
        }

        subTask = subTaskRepository.save(subTask);

        // Send notifications based on what changed
        if (statusChanged) {
            // Special notification for status change
            sendSubTaskNotifications(
                    subTask,
                    parentTask,
                    currentUser,
                    "Subtask đã đổi trạng thái",
                    "Subtask \"" + subTask.getTitle() + "\" đã chuyển từ " + oldStatus + " sang "
                            + subTask.getStatus().name(),
                    NotificationType.TASK_STATUS_CHANGED);
        } else {
            // General update notification
            sendSubTaskNotifications(
                    subTask,
                    parentTask,
                    currentUser,
                    "Subtask đã được cập nhật",
                    "Subtask \"" + subTask.getTitle() + "\" trong task \"" + parentTask.getTitle()
                            + "\" đã được cập nhật",
                    NotificationType.TASK_UPDATED);
        }

        return mapToSubTaskResponse(subTask);
    }

    @Override
    public void softDeleteSubTask(Long id, User currentUser, boolean isAdmin) {
        SubTask subTask = subTaskRepository.findByIdAndIsDelete(id, 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy subtask"));

        Task parentTask = subTask.getParentTask();
        String subTaskTitle = subTask.getTitle();

        // Check permission: ADMIN or parent task owner
        if (!isAdmin && !parentTask.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Không có quyền xóa subtask này");
        }

        subTask.setIsDelete(1);
        subTaskRepository.save(subTask);

        // Log history: Subtask deleted
        taskHistoryService.logChange(parentTask, currentUser,
                "subtask_deleted",
                "Subtask \"" + subTaskTitle + "\" tồn tại",
                "Subtask \"" + subTaskTitle + "\" đã bị xóa");

        // Send notifications for subtask deletion
        sendSubTaskNotifications(
                subTask,
                parentTask,
                currentUser,
                "Subtask đã bị xóa",
                "Subtask \"" + subTaskTitle + "\" trong task \"" + parentTask.getTitle() + "\" đã bị xóa",
                NotificationType.TASK_UPDATED);
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

    /**
     * Helper: Gửi notification cho subtask events
     * Logic:
     * - Thông báo cho parent task creator (người tạo task cha)
     * - Thông báo cho subtask assignee (người thực hiện subtask)
     * - Thông báo cho task assignee (người thực hiện task cha)
     * - Set sẽ tự động loại trùng nếu các người trên là cùng 1 người
     * - Không gửi notification cho current user (người thực hiện action)
     */
    private void sendSubTaskNotifications(SubTask subTask, Task parentTask, User currentUser,
            String title, String message, NotificationType type) {

        User subtaskAssignee = subTask.getAssignee();
        User taskAssignee = parentTask.getAssignee();
        User taskCreator = parentTask.getCreatedBy();

        // Collect unique recipients (excluding current user)
        Set<User> recipients = new HashSet<>();

        // Thông báo cho người tạo task cha
        if (taskCreator != null && !taskCreator.getId().equals(currentUser.getId())) {
            recipients.add(taskCreator);
        }

        // Thông báo cho subtask assignee
        if (subtaskAssignee != null && !subtaskAssignee.getId().equals(currentUser.getId())) {
            recipients.add(subtaskAssignee);
        }

        // Thông báo cho task assignee
        // (Set sẽ tự động loại trùng nếu taskAssignee == taskCreator hoặc
        // subtaskAssignee)
        if (taskAssignee != null && !taskAssignee.getId().equals(currentUser.getId())) {
            recipients.add(taskAssignee);
        }

        // Gửi notification cho từng người nhận
        for (User recipient : recipients) {
            notificationService.publishTaskNotification(
                    recipient,
                    title,
                    message,
                    parentTask,
                    type);
        }
    }
}
