package com.backend.quanlytasks.dto.request.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    @Email
    private String email;


    @Size(min = 6)
    private String password;
}
