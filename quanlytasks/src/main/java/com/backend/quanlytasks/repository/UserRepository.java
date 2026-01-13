package com.backend.quanlytasks.repository;

import com.backend.quanlytasks.common.enums.RoleName;
import com.backend.quanlytasks.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Tìm tất cả users có role với tên cụ thể
     * Ví dụ: findByRolesName(RoleName.ROLE_ADMIN) -> tất cả Admin users
     */
    List<User> findByRolesName(RoleName roleName);
}
