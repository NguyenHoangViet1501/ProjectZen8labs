package com.backend.quanlytasks.repository;

import com.backend.quanlytasks.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Lấy tất cả comments của một task, sắp xếp theo thời gian tạo tăng dần (cũ
     * nhất trước)
     */
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    /**
     * Đếm số comment của một task
     */
    long countByTaskId(Long taskId);
}
