package com.backend.quanlytasks.dto.response.Notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO response cho danh sách notification với phân trang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationListResponse {

    private List<NotificationResponse> notifications;

    private Long unreadCount;

    private Integer currentPage;

    private Integer totalPages;

    private Long totalElements;

    private Boolean hasNext;

    private Boolean hasPrevious;
}
