package com.backend.quanlytasks.repository;

import com.backend.quanlytasks.common.enums.RoleName;
import com.backend.quanlytasks.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
