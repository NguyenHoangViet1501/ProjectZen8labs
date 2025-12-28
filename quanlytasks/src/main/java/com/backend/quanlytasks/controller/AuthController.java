package com.backend.quanlytasks.controller;


import com.backend.quanlytasks.dto.request.Auth.LoginRequest;
import com.backend.quanlytasks.dto.request.Auth.RegisterRequest;
import com.backend.quanlytasks.dto.response.Auth.AuthResponse;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.entity.VerificationToken;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.repository.VerificationTokenRepository;
import com.backend.quanlytasks.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("Register success");
    }

    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody @Valid RegisterRequest request) {
        authService.registerAdmin(request);
        return ResponseEntity.ok("Register success");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("Xác nhận email thành công");
    }
}
