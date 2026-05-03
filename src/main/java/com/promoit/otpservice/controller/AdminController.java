package com.promoit.otpservice.controller;

import com.promoit.otpservice.dto.OtpConfigUpdateRequest;
import com.promoit.otpservice.dto.UserResponse;
import com.promoit.otpservice.model.OtpConfig;
import com.promoit.otpservice.service.OtpService;
import com.promoit.otpservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final OtpService otpService;
    private final UserService userService;

    public AdminController(OtpService otpService, UserService userService) {
        this.otpService = otpService;
        this.userService = userService;
    }

    @PutMapping("/otp-config")
    public ResponseEntity<OtpConfig> updateConfig(@Valid @RequestBody OtpConfigUpdateRequest request) {
        logger.info("PUT /api/admin/otp-config, body: {}", request);
        OtpConfig config = otpService.updateConfig(request);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllNonAdminUsers() {
        logger.info("GET /api/admin/users");
        return ResponseEntity.ok(userService.getAllNonAdminUsers());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /api/admin/users/{}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok("User and related OTP codes deleted");
    }
}