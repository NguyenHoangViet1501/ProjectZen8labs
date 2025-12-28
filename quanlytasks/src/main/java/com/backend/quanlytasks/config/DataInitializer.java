package com.backend.quanlytasks.config;

import com.backend.quanlytasks.common.enums.RoleName;
import com.backend.quanlytasks.entity.Role;
import com.backend.quanlytasks.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            roleRepository.save(Role.builder().name(RoleName.USER).build());
            roleRepository.save(Role.builder().name(RoleName.ADMIN).build());
        }
    }
}
