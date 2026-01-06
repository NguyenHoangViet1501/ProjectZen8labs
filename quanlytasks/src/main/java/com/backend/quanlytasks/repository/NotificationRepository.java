package com.backend.quanlytasks.repository;

import com.backend.quanlytasks.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lấy tất cả thông báo của một user, sắp xếp theo thời gian giảm dần
     */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /**
     * Lấy thông báo của user với phân trang
     */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /**
     * Đếm số thông báo chưa đọc của user
     */
    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
}
