package com.backend.quanlytasks.repository;

import com.backend.quanlytasks.entity.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, Long> {

    /**
     * Tìm subtask theo ID và chưa bị xóa mềm
     */
    Optional<SubTask> findByIdAndIsDelete(Long id, Integer isDelete);

    /**
     * Lấy tất cả subtasks của một task cha và chưa bị xóa
     */
    List<SubTask> findByParentTaskIdAndIsDelete(Long parentTaskId, Integer isDelete);

    /**
     * Đếm số subtask của một task
     */
    long countByParentTaskIdAndIsDelete(Long parentTaskId, Integer isDelete);
}
