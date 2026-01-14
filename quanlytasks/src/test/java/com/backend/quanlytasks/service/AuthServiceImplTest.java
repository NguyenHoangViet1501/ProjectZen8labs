package com.backend.quanlytasks.service;

import com.backend.quanlytasks.common.enums.RoleName;
import com.backend.quanlytasks.dto.request.Auth.LoginRequest;
import com.backend.quanlytasks.dto.request.Auth.RegisterRequest;
import com.backend.quanlytasks.entity.Role;
import com.backend.quanlytasks.entity.User;
import com.backend.quanlytasks.entity.VerificationToken;
import com.backend.quanlytasks.repository.RoleRepository;
import com.backend.quanlytasks.repository.UserRepository;
import com.backend.quanlytasks.repository.VerificationTokenRepository;
import com.backend.quanlytasks.service.Impl.AuthServiceImpl;
import com.backend.quanlytasks.service.Impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private EmailServiceImpl emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Test User");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleName.USER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(userRole));
    }

    @Test
    @DisplayName("Register - Success")
    void register_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act & Assert
        assertDoesNotThrow(() -> authService.register(registerRequest));
        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).save(any(VerificationToken.class));
    }

    @Test
    @DisplayName("Register - Email Already Exists - Throws Exception")
    void register_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Role Not Found - Throws Exception")
    void register_RoleNotFound_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));
        assertEquals("Role USER not found", exception.getMessage());
    }

    @Test
    @DisplayName("Login - Success")
    void login_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        // Act
        String token = authService.login(loginRequest);

        // Assert
        assertNotNull(token);
        assertEquals("jwt-token", token);
    }

    @Test
    @DisplayName("Login - Invalid Email - Throws Exception")
    void login_InvalidEmail_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("Login - Invalid Password - Throws Exception")
    void login_InvalidPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("Login - Account Not Verified - Throws Exception")
    void login_AccountNotVerified_ThrowsException() {
        // Arrange
        testUser.setEnabled(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));
        assertEquals("Account not verified", exception.getMessage());
    }

    @Test
    @DisplayName("Verify Email - Success")
    void verifyEmail_Success() {
        // Arrange
        VerificationToken token = VerificationToken.builder()
                .token("valid-token")
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();

        when(verificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act & Assert
        assertDoesNotThrow(() -> authService.verifyEmail("valid-token"));
        verify(userRepository).save(any(User.class));
        verify(verificationTokenRepository).delete(token);
    }

    @Test
    @DisplayName("Verify Email - Invalid Token - Throws Exception")
    void verifyEmail_InvalidToken_ThrowsException() {
        // Arrange
        when(verificationTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyEmail("invalid-token"));
        assertEquals("Invalid token", exception.getMessage());
    }

    @Test
    @DisplayName("Verify Email - Token Expired - Throws Exception")
    void verifyEmail_TokenExpired_ThrowsException() {
        // Arrange
        VerificationToken expiredToken = VerificationToken.builder()
                .token("expired-token")
                .user(testUser)
                .expiryDate(LocalDateTime.now().minusHours(1)) // Expired
                .build();

        when(verificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyEmail("expired-token"));
        assertEquals("Token expired", exception.getMessage());
    }

    @Test
    @DisplayName("Register Admin - Success")
    void registerAdmin_Success() {
        // Arrange
        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(RoleName.ADMIN);

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Act & Assert
        assertDoesNotThrow(() -> authService.registerAdmin(registerRequest));
        verify(userRepository).save(any(User.class));
    }
}
