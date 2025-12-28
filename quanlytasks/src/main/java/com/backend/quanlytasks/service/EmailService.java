package com.backend.quanlytasks.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token);
}
