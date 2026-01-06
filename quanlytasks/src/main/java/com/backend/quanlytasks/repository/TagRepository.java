package com.backend.quanlytasks.repository;

import com.backend.quanlytasks.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Tìm tag theo tên
     */
    Optional<Tag> findByName(String name);

    /**
     * Tìm nhiều tags theo danh sách tên
     */
    List<Tag> findByNameIn(List<String> names);

    /**
     * Kiểm tra tag có tồn tại theo tên
     */
    boolean existsByName(String name);
}
