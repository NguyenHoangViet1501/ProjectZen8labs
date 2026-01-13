package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.common.enums.TaskStatus;
import com.backend.quanlytasks.common.enums.RoleName;
import com.backend.quanlytasks.dto.request.Task.*;
import com.backend.quanlytasks.dto.response.Comment.CommentResponse;
import com.backend.quanlytasks.dto.response.SubTask.SubTaskResponse;
import com.backend.quanlytasks.dto.response.Task.TaskDetailResponse;
import com.backend.quanlytasks.dto.response.Task.TaskListResponse;
import com.backend.quanlytasks.dto.response.Task.TaskResponse;
import com.backend.quanlytasks.dto.response.TaskHistory.TaskHistoryResponse;
import com.backend.quanlytasks.entity.Tag;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.event.TaskNotificationEvent.NotificationType;
import com.backend.quanlytasks.repository.TagRepository;
import com.backend.quanlytasks.repository.TaskRepository;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.CommentService;
import com.backend.quanlytasks.service.NotificationService;
import com.backend.quanlytasks.service.SubTaskService;
import com.backend.quanlytasks.service.TaskHistoryService;
import com.backend.quanlytasks.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final TaskHistoryService taskHistoryService;
    private final SubTaskService subTaskService;
    private final CommentService commentService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, User currentUser) {
        // Validate dueDate không được trong quá khứ
        if (request.getDueDate() != null && request.getDueDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Deadline không được là thời gian trong quá khứ");
        }

        // Process tags
        Set<Tag> tags = processTags(request.getTags());

        Task task = mapToTask(request, currentUser, tags);
        task = taskRepository.save(task);

        // Log creation
        taskHistoryService.logChange(task, currentUser, "created", null, "Task được tạo");

        return mapToTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request, User currentUser, boolean isAdmin) {
        Task task = taskRepository.findByIdAndIsDelete(id, 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

        // Check permission: Only ADMIN or Creator can update task details
        if (!isAdmin && !task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Chỉ ADMIN hoặc người tạo task mới có quyền cập nhật thông tin task");
        }

        // Validate dueDate không được trong quá khứ
        if (request.getDueDate() != null && request.getDueDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Deadline không được là thời gian trong quá khứ");
        }

        // Log changes
        if (!task.getTitle().equals(request.getTitle())) {
            taskHistoryService.logChange(task, currentUser, "title", task.getTitle(), request.getTitle());
        }
        if (task.getDescription() == null || !task.getDescription().equals(request.getDescription())) {
            taskHistoryService.logChange(task, currentUser, "description",
                    task.getDescription(), request.getDescription());
        }
        if (!task.getPriority().equals(request.getPriority())) {
            taskHistoryService.logChange(task, currentUser, "priority",
                    task.getPriority().name(), request.getPriority().name());
        }

        // Update fields
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        // Update tags
        if (request.getTags() != null) {
            Set<Tag> tags = processTags(request.getTags());
            task.setTags(tags);
        }

        task = taskRepository.save(task);

        // Send notification to task creator if current user is not the creator
        if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
            notificationService.publishTaskNotification(
                    task.getCreatedBy(),
                    "Task đã được cập nhật",
                    "Task \"" + task.getTitle() + "\" đã được cập nhật bởi " + currentUser.getFullName(),
                    task,
                    NotificationType.TASK_UPDATED);
        }

        // Send notification to assignee if different from current user and creator
        if (task.getAssignee() != null
                && !task.getAssignee().getId().equals(currentUser.getId())
                && !task.getAssignee().getId().equals(task.getCreatedBy().getId())) {
            notificationService.publishTaskNotification(
                    task.getAssignee(),
                    "Task đã được cập nhật",
                    "Task \"" + task.getTitle() + "\" đã được cập nhật bởi " + currentUser.getFullName(),
                    task,
                    NotificationType.TASK_UPDATED);
        }

        return mapToTaskResponse(task);
    }

    @Override
    @Transactional
    public void softDeleteTask(Long id, User currentUser, boolean isAdmin) {
        Task task = taskRepository.findByIdAndIsDelete(id, 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

        // Check permission: only owner or admin can delete
        if (!isAdmin && !task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Chỉ người tạo hoặc Admin mới có quyền xóa task");
        }

        task.setIsDelete(1);
        taskRepository.save(task);

        // Log deletion
        taskHistoryService.logChange(task, currentUser, "deleted", null, "Task đã bị xóa");

        if (isAdmin) {
            // Admin deleted task -> notify assignee and creator
            if (task.getAssignee() != null && !task.getAssignee().getId().equals(currentUser.getId())) {
                // Force initialization for async listener
                task.getAssignee().getFcmToken();
                notificationService.publishTaskNotification(
                        task.getAssignee(),
                        "Task đã bị xóa",
                        "Task \"" + task.getTitle() + "\" đã bị xóa bởi Admin",
                        task,
                        NotificationType.TASK_UPDATED);
            }
            // Also notify creator if different from assignee and current user
            if (task.getCreatedBy() != null
                    && !task.getCreatedBy().getId().equals(currentUser.getId())
                    && (task.getAssignee() == null
                            || !task.getCreatedBy().getId().equals(task.getAssignee().getId()))) {
                // Force initialization for async listener
                task.getCreatedBy().getFcmToken();
                notificationService.publishTaskNotification(
                        task.getCreatedBy(),
                        "Task đã bị xóa",
                        "Task \"" + task.getTitle() + "\" đã bị xóa bởi Admin",
                        task,
                        NotificationType.TASK_UPDATED);
            }
        } else {
            // User deleted task -> notify all Admins
            List<User> admins = userRepository.findByRolesName(RoleName.ADMIN);
            for (User admin : admins) {
                if (!admin.getId().equals(currentUser.getId())) {
                    notificationService.publishTaskNotification(
                            admin,
                            "Task đã bị xóa",
                            "Task \"" + task.getTitle() + "\" đã bị xóa bởi " + currentUser.getFullName(),
                            task,
                            NotificationType.TASK_UPDATED);
                }
            }
        }
    }

    @Override
    public TaskListResponse getTaskList(TaskFilterRequest filter, User currentUser, boolean isAdmin) {
        Pageable pageable = PageRequest.of(
                filter.getPage() != null ? filter.getPage() : 0,
                filter.getSize() != null ? filter.getSize() : 10,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Task> taskPage;

        // Get first tag for filtering (if any)
        String tagName = (filter.getTags() != null && !filter.getTags().isEmpty())
                ? filter.getTags().get(0)
                : null;

        if (isAdmin) {
            // ADMIN: xem tất cả tasks
            taskPage = taskRepository.findAllWithFilters(
                    filter.getStatus(),
                    filter.getPriority(),
                    filter.getAssigneeId(),
                    filter.getDueDateFrom(),
                    filter.getDueDateTo(),
                    tagName,
                    pageable);
        } else {
            // USER: chỉ xem tasks của mình (tạo hoặc được assign)
            taskPage = taskRepository.findByUserWithFilters(
                    currentUser.getId(),
                    filter.getStatus(),
                    filter.getPriority(),
                    filter.getDueDateFrom(),
                    filter.getDueDateTo(),
                    tagName,
                    pageable);
        }

        List<TaskResponse> taskResponses = taskPage.getContent().stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());

        return TaskListResponse.builder()
                .tasks(taskResponses)
                .currentPage(taskPage.getNumber())
                .totalPages(taskPage.getTotalPages())
                .totalElements(taskPage.getTotalElements())
                .pageSize(taskPage.getSize())
                .hasNext(taskPage.hasNext())
                .hasPrevious(taskPage.hasPrevious())
                .build();
    }

    @Override
    public TaskDetailResponse getTaskDetail(Long id, User currentUser, boolean isAdmin) {
        // Admin can view deleted tasks, users cannot
        Task task;
        if (isAdmin) {
            task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));
        } else {
            task = taskRepository.findByIdAndIsDelete(id, 0)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));
        }

        // Get related data first (needed for permission check)
        List<SubTaskResponse> subtasks = subTaskService.getSubTasksByTaskId(id);

        // Check if user is assigned to any subtask
        boolean isSubtaskAssignee = subtasks.stream()
                .anyMatch(st -> st.getAssigneeId() != null && st.getAssigneeId().equals(currentUser.getId()));

        // Check permission: admin, creator, task assignee, or subtask assignee
        if (!isAdmin && !task.getCreatedBy().getId().equals(currentUser.getId())
                && (task.getAssignee() == null || !task.getAssignee().getId().equals(currentUser.getId()))
                && !isSubtaskAssignee) {
            throw new RuntimeException("Không có quyền xem task này");
        }

        // Get remaining related data
        List<CommentResponse> comments = commentService.getCommentsByTaskId(id);
        List<TaskHistoryResponse> history = taskHistoryService.getTaskHistory(id);

        return mapToTaskDetailResponse(task, subtasks, comments, history);
    }

    @Override
    @Transactional
    public TaskResponse assignTask(Long id, AssignTaskRequest request, User currentUser) {
        Task task = taskRepository.findByIdAndIsDelete(id, 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

        // Chỉ người tạo task mới được giao task cho người khác
        if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền giao task này. Chỉ người tạo task mới được giao.");
        }

        User assignee = userRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người được giao"));

        // Log change
        String oldAssignee = task.getAssignee() != null ? task.getAssignee().getFullName() : "Chưa giao";
        taskHistoryService.logChange(task, currentUser, "assignee", oldAssignee, assignee.getFullName());

        task.setAssignee(assignee);
        task = taskRepository.save(task);

        // Send notification to assignee
        notificationService.publishTaskNotification(
                assignee,
                "Bạn được giao task mới",
                "Bạn đã được giao task: " + task.getTitle(),
                task,
                NotificationType.TASK_ASSIGNED);

        return mapToTaskResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long id, UpdateTaskStatusRequest request, User currentUser, boolean isAdmin) {
        Task task = taskRepository.findByIdAndIsDelete(id, 0)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

        // Check permission: ADMIN, Creator, or Assignee
        boolean isCreator = task.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(currentUser.getId());

        if (!isAdmin && !isCreator && !isAssignee) {
            throw new RuntimeException("Bạn không có quyền cập nhật trạng thái task này");
        }

        String oldStatus = task.getStatus().name();
        String newStatus = request.getStatus().name();

        // Log change
        taskHistoryService.logChange(task, currentUser, "status", oldStatus, newStatus);

        task.setStatus(request.getStatus());
        task = taskRepository.save(task);

        // Send notification to task creator about status change
        if (!task.getCreatedBy().getId().equals(currentUser.getId())) {
            notificationService.publishTaskNotification(
                    task.getCreatedBy(),
                    "Task đã được cập nhật trạng thái",
                    "Task \"" + task.getTitle() + "\" đã chuyển từ " + oldStatus + " sang " + newStatus,
                    task,
                    NotificationType.TASK_STATUS_CHANGED);
        }

        // Send notification to assignee if different from current user and creator
        if (task.getAssignee() != null
                && !task.getAssignee().getId().equals(currentUser.getId())
                && !task.getAssignee().getId().equals(task.getCreatedBy().getId())) {
            notificationService.publishTaskNotification(
                    task.getAssignee(),
                    "Task đã được cập nhật trạng thái",
                    "Task \"" + task.getTitle() + "\" đã chuyển từ " + oldStatus + " sang " + newStatus,
                    task,
                    NotificationType.TASK_STATUS_CHANGED);
        }

        return mapToTaskResponse(task);
    }

    /**
     * Xử lý tags: tạo mới nếu chưa tồn tại
     */
    private Set<Tag> processTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<Tag> tags = new HashSet<>();
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> {
                        Tag newTag = Tag.builder()
                                .name(tagName)
                                .build();
                        return tagRepository.save(newTag);
                    });
            tags.add(tag);
        }
        return tags;
    }

    /**
     * Mapper: CreateTaskRequest -> Task
     */
    private Task mapToTask(CreateTaskRequest request, User creator, Set<Tag> tags) {
        return Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .createdBy(creator)
                .tags(tags)
                .isDelete(0)
                .build();
    }

    /**
     * Mapper: Task -> TaskResponse
     */
    private TaskResponse mapToTaskResponse(Task task) {
        List<String> tagNames = task.getTags() != null
                ? task.getTags().stream().map(Tag::getName).collect(Collectors.toList())
                : new ArrayList<>();

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdById(task.getCreatedBy().getId())
                .createdByName(task.getCreatedBy().getFullName())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .tags(tagNames)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .isDeleted(task.getIsDelete() == 1)
                .build();
    }

    /**
     * Mapper: Task -> TaskDetailResponse (bao gồm subtasks, comments, history)
     */
    private TaskDetailResponse mapToTaskDetailResponse(Task task,
            List<SubTaskResponse> subtasks,
            List<CommentResponse> comments,
            List<TaskHistoryResponse> history) {

        List<String> tagNames = task.getTags() != null
                ? task.getTags().stream().map(Tag::getName).collect(Collectors.toList())
                : new ArrayList<>();

        return TaskDetailResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .createdById(task.getCreatedBy().getId())
                .createdByName(task.getCreatedBy().getFullName())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .tags(tagNames)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .subtasks(subtasks)
                .comments(comments)
                .history(history)
                .isDeleted(task.getIsDelete() == 1)
                .build();
    }

    @Override
    public byte[] exportTasksToExcel(User currentUser, boolean isAdmin) {
        // Lấy danh sách tasks
        List<Task> tasks;
        if (isAdmin) {
            tasks = taskRepository.findAllByIsDelete(0);
        } else {
            tasks = taskRepository.findByCreatedByIdOrAssigneeIdAndIsDelete(
                    currentUser.getId(), currentUser.getId(), 0);
        }

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {

            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("Tasks");

            // Tạo style cho header
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            // Tạo style cho date
            org.apache.poi.xssf.usermodel.XSSFCellStyle dateStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));

            // Header row
            String[] headers = { "ID", "Tiêu đề", "Mô tả", "Trạng thái", "Độ ưu tiên",
                    "Người được giao", "Ngày deadline", "Ngày tạo" };
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Task task : tasks) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(task.getId());
                row.createCell(1).setCellValue(task.getTitle());
                row.createCell(2).setCellValue(task.getDescription() != null ? task.getDescription() : "");
                row.createCell(3).setCellValue(task.getStatus().name());
                row.createCell(4).setCellValue(task.getPriority().name());
                row.createCell(5)
                        .setCellValue(task.getAssignee() != null ? task.getAssignee().getFullName() : "Chưa giao");

                // Due date
                org.apache.poi.ss.usermodel.Cell dueDateCell = row.createCell(6);
                if (task.getDueDate() != null) {
                    dueDateCell.setCellValue(java.sql.Timestamp.valueOf(task.getDueDate()));
                    dueDateCell.setCellStyle(dateStyle);
                } else {
                    dueDateCell.setCellValue("Không có");
                }

                // Created at
                org.apache.poi.ss.usermodel.Cell createdAtCell = row.createCell(7);
                if (task.getCreatedAt() != null) {
                    createdAtCell.setCellValue(java.sql.Timestamp.valueOf(task.getCreatedAt()));
                    createdAtCell.setCellStyle(dateStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convert to byte array
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (java.io.IOException e) {
            throw new RuntimeException("Lỗi khi xuất file Excel: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void restoreTask(Long id, User currentUser) {
        // Find task including deleted ones (is_delete = 1)
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

        if (task.getIsDelete() == 0) {
            throw new RuntimeException("Task này chưa bị xóa");
        }

        // Restore task
        task.setIsDelete(0);
        taskRepository.save(task);

        // Log restore
        taskHistoryService.logChange(task, currentUser, "restored", "Task đã bị xóa", "Task đã được hoàn tác");

        // Notify assignee that task has been restored
        if (task.getAssignee() != null && !task.getAssignee().getId().equals(currentUser.getId())) {
            // Force initialization
            task.getAssignee().getFcmToken();
            notificationService.publishTaskNotification(
                    task.getAssignee(),
                    "Task đã được hoàn tác",
                    "Task \"" + task.getTitle() + "\" đã được hoàn tác bởi Admin",
                    task,
                    NotificationType.TASK_UPDATED);
        }

        // Also notify task creator if different from assignee and current user
        if (task.getCreatedBy() != null
                && !task.getCreatedBy().getId().equals(currentUser.getId())
                && (task.getAssignee() == null || !task.getCreatedBy().getId().equals(task.getAssignee().getId()))) {
            // Force initialization
            task.getCreatedBy().getFcmToken();
            notificationService.publishTaskNotification(
                    task.getCreatedBy(),
                    "Task đã được hoàn tác",
                    "Task \"" + task.getTitle() + "\" đã được hoàn tác bởi Admin",
                    task,
                    NotificationType.TASK_UPDATED);
        }
    }
}
