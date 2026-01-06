package com.backend.quanlytasks.repository;

import com.backend.quanlytasks.common.enums.Priority;
import com.backend.quanlytasks.common.enums.TaskStatus;
import com.backend.quanlytasks.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

        /**
         * Tìm task theo ID và chưa bị xóa mềm
         */
        Optional<Task> findByIdAndIsDelete(Long id, Integer isDelete);

        /**
         * Lấy danh sách task với phân trang và filter
         * Dùng cho ADMIN (xem tất cả)
         */
        @Query("SELECT t FROM Task t WHERE t.isDelete = 0 " +
                        "AND (:status IS NULL OR t.status = :status) " +
                        "AND (:priority IS NULL OR t.priority = :priority) " +
                        "AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId) " +
                        "AND (:dueDateFrom IS NULL OR t.dueDate >= :dueDateFrom) " +
                        "AND (:dueDateTo IS NULL OR t.dueDate <= :dueDateTo)")
        Page<Task> findAllWithFilters(
                        @Param("status") TaskStatus status,
                        @Param("priority") Priority priority,
                        @Param("assigneeId") Long assigneeId,
                        @Param("dueDateFrom") LocalDateTime dueDateFrom,
                        @Param("dueDateTo") LocalDateTime dueDateTo,
                        Pageable pageable);

        /**
         * Lấy danh sách task của USER (task do họ tạo hoặc được assign)
         */
        @Query("SELECT t FROM Task t WHERE t.isDelete = 0 " +
                        "AND (t.createdBy.id = :userId OR t.assignee.id = :userId) " +
                        "AND (:status IS NULL OR t.status = :status) " +
                        "AND (:priority IS NULL OR t.priority = :priority) " +
                        "AND (:dueDateFrom IS NULL OR t.dueDate >= :dueDateFrom) " +
                        "AND (:dueDateTo IS NULL OR t.dueDate <= :dueDateTo)")
        Page<Task> findByUserWithFilters(
                        @Param("userId") Long userId,
                        @Param("status") TaskStatus status,
                        @Param("priority") Priority priority,
                        @Param("dueDateFrom") LocalDateTime dueDateFrom,
                        @Param("dueDateTo") LocalDateTime dueDateTo,
                        Pageable pageable);

        /**
         * Lấy tất cả tasks chưa xóa (phân trang)
         */
        Page<Task> findByIsDelete(Integer isDelete, Pageable pageable);

        /**
         * Lấy tất cả tasks chưa xóa (không phân trang - cho export Excel)
         */
        List<Task> findAllByIsDelete(Integer isDelete);

        /**
         * Lấy tasks của user (tạo hoặc được assign) - cho export Excel
         */
        @Query("SELECT t FROM Task t WHERE t.isDelete = :isDelete " +
                        "AND (t.createdBy.id = :createdById OR t.assignee.id = :assigneeId)")
        List<Task> findByCreatedByIdOrAssigneeIdAndIsDelete(
                        @Param("createdById") Long createdById,
                        @Param("assigneeId") Long assigneeId,
                        @Param("isDelete") Integer isDelete);
}
