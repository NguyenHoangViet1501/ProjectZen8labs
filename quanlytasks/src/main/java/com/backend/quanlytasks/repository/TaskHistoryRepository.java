package com.backend.quanlytasks.repository;

import com.backend.quanlytasks.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    /**
     * Lấy tất cả lịch sử thay đổi của một task, sắp xếp theo thời gian giảm dần
     */
    List<TaskHistory> findByTaskIdOrderByChangedAtDesc(Long taskId);
}
