package com.promoit.otpservice.service;

import com.promoit.otpservice.dto.LoginRequest;
import com.promoit.otpservice.dto.LoginResponse;
import com.promoit.otpservice.dto.RegisterRequest;
import com.promoit.otpservice.exception.ServiceException;
import com.promoit.otpservice.model.User;
import com.promoit.otpservice.repository.UserRepository;
import com.promoit.otpservice.security.JwtTokenProvider;
import com.promoit.otpservice.util.PasswordEncoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void register(RegisterRequest request) {
        logger.info("Registering user: {}", request.getUsername());
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ServiceException("Username already exists");
        }
        String role = request.getRole().toUpperCase();
        if ("ADMIN".equals(role)) {
            if (userRepository.existsByRole("ADMIN")) {
                throw new ServiceException("Admin already exists");
            }
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordEncoderUtil.encode(request.getPassword()));
        user.setRole(role);
        userRepository.save(user);
        logger.info("User registered: {} with role {}", user.getUsername(), role);
    }

    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt for user: {}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ServiceException("Invalid credentials"));
        if (!PasswordEncoderUtil.matches(request.getPassword(), user.getPassword())) {
            throw new ServiceException("Invalid credentials");
        }
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        logger.info("User {} logged in successfully", user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getRole());
    }
}