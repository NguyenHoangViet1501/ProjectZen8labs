package com.backend.quanlytasks.event;

import com.backend.quanlytasks.entity.Task;
import com.backend.quanlytasks.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event được publish khi có thông báo liên quan đến Task
 * Các loại thông báo: giao task, thay đổi trạng thái, cập nhật task
 */
@Getter
public class TaskNotificationEvent extends ApplicationEvent {

    private final User recipient;
    private final String title;
    private final String message;
    private final Task relatedTask;
    private final NotificationType type;

    public TaskNotificationEvent(Object source, User recipient, String title,
            String message, Task relatedTask, NotificationType type) {
        super(source);
        this.recipient = recipient;
        this.title = title;
        this.message = message;
        this.relatedTask = relatedTask;
        this.type = type;
    }

    /**
     * Các loại thông báo task
     */
    public enum NotificationType {
        TASK_ASSIGNED, // Task được giao cho user
        TASK_STATUS_CHANGED, // Trạng thái task thay đổi
        TASK_UPDATED, // Task được cập nhật
        TASK_COMMENTED // Có comment mới trong task
    }
}
