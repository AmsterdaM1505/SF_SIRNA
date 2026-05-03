package com.promoit.otpservice;

import com.promoit.otpservice.dto.LoginRequest;
import com.promoit.otpservice.dto.LoginResponse;
import com.promoit.otpservice.dto.RegisterRequest;
import com.promoit.otpservice.exception.ServiceException;
import com.promoit.otpservice.model.User;
import com.promoit.otpservice.repository.UserRepository;
import com.promoit.otpservice.security.JwtTokenProvider;
import com.promoit.otpservice.service.AuthService;
import com.promoit.otpservice.util.PasswordEncoderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtTokenProvider);
    }

    @Test
    void register_shouldCreateUser() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("user1");
        req.setPassword("pass");
        req.setRole("USER");

        when(userRepository.findByUsername("user1")).thenReturn(Optional.empty());
        // Строку ниже УДАЛИТЬ (она не используется для USER)
        // when(userRepository.existsByRole("ADMIN")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(new User());

        assertDoesNotThrow(() -> authService.register(req));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowIfUsernameExists() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("user1");
        req.setPassword("pass");
        req.setRole("USER");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(new User()));

        assertThrows(ServiceException.class, () -> authService.register(req));
    }

    @Test
    void register_shouldThrowIfAdminAlreadyExists() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("admin2");
        req.setPassword("pass");
        req.setRole("ADMIN");
        when(userRepository.findByUsername("admin2")).thenReturn(Optional.empty());
        when(userRepository.existsByRole("ADMIN")).thenReturn(true);

        assertThrows(ServiceException.class, () -> authService.register(req));
    }

    @Test
    void login_shouldReturnToken() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        user.setPassword(PasswordEncoderUtil.encode("pass"));
        user.setRole("USER");

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(1L, "user1", "USER")).thenReturn("token123");

        LoginRequest req = new LoginRequest();
        req.setUsername("user1");
        req.setPassword("pass");

        LoginResponse resp = authService.login(req);
        assertEquals("token123", resp.getToken());
        assertEquals("user1", resp.getUsername());
    }

    @Test
    void login_shouldThrowIfInvalidPassword() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        user.setPassword(PasswordEncoderUtil.encode("pass"));
        user.setRole("USER");

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setUsername("user1");
        req.setPassword("wrong");

        assertThrows(ServiceException.class, () -> authService.login(req));
    }
}