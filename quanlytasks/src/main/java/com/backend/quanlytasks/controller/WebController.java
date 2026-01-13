package com.backend.quanlytasks.controller;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.TaskStatus;
import com.backend.quanlytasks.dto.request.Task.CreateTaskRequest;
import com.backend.quanlytasks.dto.request.Task.TaskFilterRequest;
import com.backend.quanlytasks.dto.request.Task.UpdateTaskRequest;
import com.backend.quanlytasks.dto.request.Task.UpdateTaskStatusRequest;
import com.backend.quanlytasks.dto.response.Notification.NotificationListResponse;
import com.backend.quanlytasks.dto.response.Task.TaskDetailResponse;
import com.backend.quanlytasks.dto.response.Task.TaskListResponse;
import com.backend.quanlytasks.entity.Tag;
import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.repository.TagRepository;
import com.backend.quanlytasks.repository.TaskRepository;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.service.CommentService;
import com.backend.quanlytasks.service.NotificationService;
import com.backend.quanlytasks.service.SubTaskService;
import com.backend.quanlytasks.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller cho giao diện web Thymeleaf
 */
@Controller
@RequiredArgsConstructor
public class WebController {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final CommentService commentService;
    private final SubTaskService subTaskService;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;

    // ================ AUTH ================

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {
        // Check if email exists
        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng!");
            return "redirect:/register";
        }

        // Create user
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEnabled(true);
        userRepository.save(user);

        return "redirect:/login?registered";
    }

    // ================ TASKS ================

    @GetMapping("/")
    public String home() {
        return "redirect:/tasks";
    }

    @GetMapping("/tasks")
    public String taskList(@RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String dueDateFrom,
            @RequestParam(required = false) String dueDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        // Build filter request
        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setPage(page);
        filter.setSize(size);
        if (status != null && !status.isEmpty()) {
            filter.setStatus(TaskStatus.valueOf(status));
        }
        if (priority != null && !priority.isEmpty()) {
            filter.setPriority(Priority.valueOf(priority));
        }
        if (tag != null && !tag.isEmpty()) {
            filter.setTags(java.util.Arrays.asList(tag));
        }
        if (dueDateFrom != null && !dueDateFrom.isEmpty()) {
            filter.setDueDateFrom(LocalDateTime.parse(dueDateFrom + "T00:00:00"));
        }
        if (dueDateTo != null && !dueDateTo.isEmpty()) {
            filter.setDueDateTo(LocalDateTime.parse(dueDateTo + "T23:59:59"));
        }

        // Get tasks
        TaskListResponse tasks = taskService.getTaskList(filter, currentUser, isAdmin);

        // Calculate stats
        Map<String, Long> stats = calculateStats(currentUser, isAdmin);

        // Get unread notifications count
        NotificationListResponse notifResponse = notificationService.getNotifications(currentUser, 0, 1);

        // Get all tags for filter dropdown
        List<Tag> allTags = tagRepository.findAll();

        model.addAttribute("tasks", new PageWrapper<>(tasks));
        model.addAttribute("stats", stats);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("unreadCount", notifResponse.getUnreadCount());
        model.addAttribute("allTags", allTags);
        model.addAttribute("currentPage", "tasks");

        return "task/list";
    }

    @GetMapping("/tasks/{id}")
    public String taskDetail(@PathVariable Long id, Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        TaskDetailResponse task = taskService.getTaskDetail(id, currentUser, isAdmin);

        // Lấy danh sách users để giao task
        List<User> users = userRepository.findAll();

        // Check if user can update task status (not just subtask assignee)
        boolean canUpdateTaskStatus = isAdmin
                || task.getCreatedById().equals(currentUser.getId())
                || (task.getAssigneeId() != null && task.getAssigneeId().equals(currentUser.getId()));

        model.addAttribute("task", task);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("users", users);
        model.addAttribute("canUpdateTaskStatus", canUpdateTaskStatus);

        return "task/detail";
    }

    @GetMapping("/tasks/new")
    public String newTaskForm(Model model) {
        model.addAttribute("task", null);
        return "task/form";
    }

    @PostMapping("/tasks/create")
    public String createTask(@RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String priority,
            @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) String tags,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);

        try {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setPriority(Priority.valueOf(priority));

            if (dueDate != null && !dueDate.isEmpty()) {
                request.setDueDate(LocalDateTime.parse(dueDate));
            }

            if (tags != null && !tags.isEmpty()) {
                List<String> tagList = Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                request.setTags(tagList);
            }

            taskService.createTask(request, currentUser);
            redirectAttributes.addFlashAttribute("success", "Tạo task thành công!");
            return "redirect:/tasks";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/tasks/new";
        }
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTaskForm(@PathVariable Long id, Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        TaskDetailResponse task = taskService.getTaskDetail(id, currentUser, isAdmin);
        model.addAttribute("task", task);

        return "task/form";
    }

    @PostMapping("/tasks/{id}/update")
    public String updateTask(@PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String priority,
            @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) String tags,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        try {
            UpdateTaskRequest request = new UpdateTaskRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setPriority(Priority.valueOf(priority));

            if (dueDate != null && !dueDate.isEmpty()) {
                request.setDueDate(LocalDateTime.parse(dueDate));
            }

            if (tags != null && !tags.isEmpty()) {
                List<String> tagList = Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                request.setTags(tagList);
            }

            taskService.updateTask(id, request, currentUser, isAdmin);
            redirectAttributes.addFlashAttribute("success", "Cập nhật task thành công!");
            return "redirect:/tasks/" + id;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/tasks/" + id + "/edit";
        }
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        try {
            taskService.softDeleteTask(id, currentUser, isAdmin);
            redirectAttributes.addFlashAttribute("success", "Xóa task thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/tasks";
    }

    /**
     * Toggle delete status for Admin only
     * If task is active (is_delete=0) -> soft delete it
     * If task is deleted (is_delete=1) -> restore it
     */
    @PostMapping("/tasks/{id}/toggle-delete")
    public String toggleTaskDelete(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        if (!isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Chỉ Admin mới có quyền thực hiện thao tác này");
            return "redirect:/tasks";
        }

        try {
            // Check current state of task
            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy task"));

            if (task.getIsDelete() == 0) {
                // Active -> Delete
                taskService.softDeleteTask(id, currentUser, isAdmin);
                redirectAttributes.addFlashAttribute("success", "Đã xóa task!");
            } else {
                // Deleted -> Restore
                taskService.restoreTask(id, currentUser);
                redirectAttributes.addFlashAttribute("success", "Đã hoàn tác task!");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/tasks";
    }

    @PostMapping("/tasks/{id}/status")
    public String updateTaskStatus(@PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest();
        request.setStatus(TaskStatus.valueOf(status));

        taskService.updateTaskStatus(id, request, currentUser, isAdmin);

        return "redirect:/tasks/" + id;
    }

    @PostMapping("/tasks/{id}/comment")
    public String addComment(@PathVariable Long id,
            @RequestParam String content,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        com.backend.quanlytasks.dto.request.Comment.CreateCommentRequest request = new com.backend.quanlytasks.dto.request.Comment.CreateCommentRequest();
        request.setTaskId(id);
        request.setContent(content);
        commentService.createComment(request, currentUser);

        return "redirect:/tasks/" + id;
    }

    // ================ ASSIGN TASK ================

    @PostMapping("/tasks/{id}/assign")
    public String assignTask(@PathVariable Long id,
            @RequestParam Long assigneeId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);

        try {
            com.backend.quanlytasks.dto.request.Task.AssignTaskRequest request = new com.backend.quanlytasks.dto.request.Task.AssignTaskRequest();
            request.setAssigneeId(assigneeId);

            taskService.assignTask(id, request, currentUser);
            redirectAttributes.addFlashAttribute("success", "Giao task thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/tasks/" + id;
    }

    // ================ SUBTASK ================

    @PostMapping("/tasks/{id}/subtask")
    public String createSubTask(@PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long assigneeId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        try {
            com.backend.quanlytasks.dto.request.SubTask.CreateSubTaskRequest request = new com.backend.quanlytasks.dto.request.SubTask.CreateSubTaskRequest();
            request.setParentTaskId(id);
            request.setTitle(title);
            request.setDescription(description);
            request.setAssigneeId(assigneeId);

            subTaskService.createSubTask(request, currentUser, isAdmin);
            redirectAttributes.addFlashAttribute("success", "Tạo subtask thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/tasks/" + id;
    }

    @PostMapping("/tasks/{taskId}/subtask/{subtaskId}/delete")
    public String deleteSubTask(@PathVariable Long taskId,
            @PathVariable Long subtaskId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        try {
            subTaskService.softDeleteSubTask(subtaskId, currentUser, isAdmin);
            redirectAttributes.addFlashAttribute("success", "Xóa subtask thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/tasks/" + taskId;
    }

    @PostMapping("/tasks/{taskId}/subtask/{subtaskId}/status")
    public String updateSubTaskStatus(@PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @RequestParam String status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        try {
            com.backend.quanlytasks.dto.request.SubTask.UpdateSubTaskRequest request = new com.backend.quanlytasks.dto.request.SubTask.UpdateSubTaskRequest();
            request.setStatus(TaskStatus.valueOf(status));

            subTaskService.updateSubTask(subtaskId, request, currentUser, isAdmin);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái subtask thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/tasks/" + taskId;
    }

    @PostMapping("/tasks/{taskId}/subtask/{subtaskId}/update")
    public String updateSubTask(@PathVariable Long taskId,
            @PathVariable Long subtaskId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        try {
            com.backend.quanlytasks.dto.request.SubTask.UpdateSubTaskRequest request = new com.backend.quanlytasks.dto.request.SubTask.UpdateSubTaskRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setAssigneeId(assigneeId);
            if (status != null && !status.isEmpty()) {
                request.setStatus(TaskStatus.valueOf(status));
            }

            subTaskService.updateSubTask(subtaskId, request, currentUser, isAdmin);
            redirectAttributes.addFlashAttribute("success", "Cập nhật subtask thành công!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/tasks/" + taskId;
    }

    // ================ NOTIFICATIONS ================

    @GetMapping("/notifications")
    public String notificationList(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        NotificationListResponse response = notificationService.getNotifications(currentUser, page, size);

        model.addAttribute("notifications", new PageWrapper<>(response));
        model.addAttribute("unreadCount", response.getUnreadCount());
        model.addAttribute("currentPage", "notifications");

        return "notification/list";
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        notificationService.markAllAsRead(currentUser);
        return "redirect:/notifications";
    }

    // ================ EXPORT ================

    @GetMapping("/tasks/export")
    public org.springframework.http.ResponseEntity<byte[]> exportTasksToExcel(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        boolean isAdmin = isAdmin(authentication);

        byte[] excelData = taskService.exportTasksToExcel(currentUser, isAdmin);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "tasks_report.xlsx");
        headers.setContentLength(excelData.length);

        return new org.springframework.http.ResponseEntity<>(excelData, headers,
                org.springframework.http.HttpStatus.OK);
    }

    // ================ FCM TOKEN ================

    @PostMapping("/fcm-token")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> updateFcmToken(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            String fcmToken = payload.get("fcmToken");

            if (fcmToken != null && !fcmToken.isEmpty()) {
                notificationService.updateFcmToken(currentUser, fcmToken);
                return org.springframework.http.ResponseEntity.ok("FCM token updated");
            }
            return org.springframework.http.ResponseEntity.badRequest().body("Invalid token");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ================ HELPERS ================

    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Map<String, Long> calculateStats(User currentUser, boolean isAdmin) {
        Map<String, Long> stats = new HashMap<>();

        if (isAdmin) {
            // Admin sees all tasks
            stats.put("todo", taskRepository.countByStatusAndIsDelete(TaskStatus.TODO, 0));
            stats.put("inProgress", taskRepository.countByStatusAndIsDelete(TaskStatus.IN_PROGRESS, 0));
            stats.put("done", taskRepository.countByStatusAndIsDelete(TaskStatus.DONE, 0));
            stats.put("cancelled", taskRepository.countByStatusAndIsDelete(TaskStatus.CANCELLED, 0));
        } else {
            // User sees only their own tasks (created, assigned, or subtask assigned)
            Long userId = currentUser.getId();
            stats.put("todo", taskRepository.countByStatusForUser(TaskStatus.TODO, userId));
            stats.put("inProgress", taskRepository.countByStatusForUser(TaskStatus.IN_PROGRESS, userId));
            stats.put("done", taskRepository.countByStatusForUser(TaskStatus.DONE, userId));
            stats.put("cancelled", taskRepository.countByStatusForUser(TaskStatus.CANCELLED, userId));
        }

        return stats;
    }

    /**
     * Wrapper class to make TaskListResponse work with Thymeleaf pagination
     */
    public static class PageWrapper<T> {
        private final Object content;
        private final int number;
        private final int totalPages;
        private final long totalElements;
        private final boolean first;
        private final boolean last;

        public PageWrapper(TaskListResponse response) {
            this.content = response.getTasks();
            this.number = response.getCurrentPage();
            this.totalPages = response.getTotalPages();
            this.totalElements = response.getTotalElements();
            this.first = !response.getHasPrevious();
            this.last = !response.getHasNext();
        }

        public PageWrapper(NotificationListResponse response) {
            this.content = response.getNotifications();
            this.number = response.getCurrentPage();
            this.totalPages = response.getTotalPages();
            this.totalElements = response.getTotalElements();
            this.first = !response.getHasPrevious();
            this.last = !response.getHasNext();
        }

        public Object getContent() {
            return content;
        }

        public int getNumber() {
            return number;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public boolean isFirst() {
            return first;
        }

        public boolean isLast() {
            return last;
        }
    }
}
