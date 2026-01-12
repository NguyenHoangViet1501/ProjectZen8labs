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
        @Query("SELECT DISTINCT t FROM Task t LEFT JOIN t.tags tag WHERE t.isDelete = 0 " +
                        "AND (:status IS NULL OR t.status = :status) " +
                        "AND (:priority IS NULL OR t.priority = :priority) " +
                        "AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId) " +
                        "AND (:dueDateFrom IS NULL OR t.dueDate >= :dueDateFrom) " +
                        "AND (:dueDateTo IS NULL OR t.dueDate <= :dueDateTo) " +
                        "AND (:tagName IS NULL OR tag.name = :tagName)")
        Page<Task> findAllWithFilters(
                        @Param("status") TaskStatus status,
                        @Param("priority") Priority priority,
                        @Param("assigneeId") Long assigneeId,
                        @Param("dueDateFrom") LocalDateTime dueDateFrom,
                        @Param("dueDateTo") LocalDateTime dueDateTo,
                        @Param("tagName") String tagName,
                        Pageable pageable);

        /**
         * Lấy danh sách task của USER (task do họ tạo, được assign, hoặc được assign
         * subtask)
         */
        @Query("SELECT DISTINCT t FROM Task t LEFT JOIN t.tags tag WHERE t.isDelete = 0 " +
                        "AND (t.createdBy.id = :userId OR t.assignee.id = :userId " +
                        "     OR EXISTS (SELECT 1 FROM SubTask st WHERE st.parentTask = t AND st.assignee.id = :userId AND st.isDelete = 0)) "
                        +
                        "AND (:status IS NULL OR t.status = :status) " +
                        "AND (:priority IS NULL OR t.priority = :priority) " +
                        "AND (:dueDateFrom IS NULL OR t.dueDate >= :dueDateFrom) " +
                        "AND (:dueDateTo IS NULL OR t.dueDate <= :dueDateTo) " +
                        "AND (:tagName IS NULL OR tag.name = :tagName)")
        Page<Task> findByUserWithFilters(
                        @Param("userId") Long userId,
                        @Param("status") TaskStatus status,
                        @Param("priority") Priority priority,
                        @Param("dueDateFrom") LocalDateTime dueDateFrom,
                        @Param("dueDateTo") LocalDateTime dueDateTo,
                        @Param("tagName") String tagName,
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

        /**
         * Đếm số task theo status cho ADMIN (tất cả task)
         */
        long countByStatusAndIsDelete(TaskStatus status, Integer isDelete);

        /**
         * Đếm số task theo status cho USER (task do họ tạo, được assign, hoặc được
         * assign subtask)
         */
        @Query("SELECT COUNT(DISTINCT t) FROM Task t WHERE t.isDelete = 0 " +
                        "AND t.status = :status " +
                        "AND (t.createdBy.id = :userId OR t.assignee.id = :userId " +
                        "     OR EXISTS (SELECT 1 FROM SubTask st WHERE st.parentTask = t AND st.assignee.id = :userId AND st.isDelete = 0))")
        long countByStatusForUser(@Param("status") TaskStatus status, @Param("userId") Long userId);
}
