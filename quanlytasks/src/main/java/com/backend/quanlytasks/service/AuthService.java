package com.backend.quanlytasks.service;

import com.backend.quanlytasks.dto.request.Auth.LoginRequest;
import com.backend.quanlytasks.dto.request.Auth.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest request);

    String login(LoginRequest request);

    void verifyEmail(String token);

    void registerAdmin(RegisterRequest request);
}
