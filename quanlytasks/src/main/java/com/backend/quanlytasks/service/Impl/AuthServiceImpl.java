package com.backend.quanlytasks.service.Impl;

import com.backend.quanlytasks.common.enums.RoleName;
import com.backend.quanlytasks.dto.request.Auth.LoginRequest;
import com.backend.quanlytasks.dto.request.Auth.RegisterRequest;
import com.backend.quanlytasks.entity.Role;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.entity.VerificationToken;
import com.backend.quanlytasks.repository.RoleRepository;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.repository.VerificationTokenRepository;
import com.backend.quanlytasks.service.AuthService;
import com.backend.quanlytasks.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailServiceImpl emailService;

    @Override
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        User user = mapRegisterRequestToUser(request, userRole);

        userRepository.save(user);

        createAndSendVerificationToken(user);
    }

    @Override
    public void registerAdmin(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role userRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

        User user = mapRegisterRequestToUser(request, userRole);

        userRepository.save(user);

        createAndSendVerificationToken(user);
    }


    private void createAndSendVerificationToken(User user) {

        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();

        verificationTokenRepository.save(verificationToken);

        // gửi mail
        emailService.sendVerificationEmail(user.getEmail(), token);
    }
    @Override
    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified");
        }

        return jwtService.generateToken(user);
    }
    @Override
    public void verifyEmail(String token) {

        VerificationToken verificationToken = verificationTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        verificationTokenRepository.delete(verificationToken);
    }

    /**
     * Mapper rõ ràng: RegisterRequest -> User
     */
    private User mapRegisterRequestToUser(RegisterRequest request, Role userRole) {

        User user = new User();

        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEnabled(false);
        user.setRoles(Set.of(userRole));

        return user;
    }
}
