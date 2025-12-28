package com.backend.quanlytasks.service;

import com.backend.quanlytasks.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateToken(User user);

    String extractUsername(String token);

    boolean validateToken(String token, UserDetails userDetails);
}
