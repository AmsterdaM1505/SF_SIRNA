package com.promoit.otpservice.controller;

import com.promoit.otpservice.dto.OtpGenerateRequest;
import com.promoit.otpservice.dto.OtpValidateRequest;
import com.promoit.otpservice.service.OtpService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('USER')")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final OtpService otpService;

    public UserController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/otp/generate")
    public ResponseEntity<String> generateOtp(@Valid @RequestBody OtpGenerateRequest request,
                                              Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        logger.info("POST /api/user/otp/generate, operationId={}, channels={}, userId={}",
                request.getOperationId(), request.getChannels(), userId);
        otpService.generateAndSend(userId, request.getOperationId(), request.getChannels(),
                request.getEmail(), request.getPhone(), request.getTelegramChatId());
        return ResponseEntity.ok("OTP generated and sent");
    }

    @PostMapping("/otp/validate")
    public ResponseEntity<String> validateOtp(@Valid @RequestBody OtpValidateRequest request) {
        logger.info("POST /api/user/otp/validate, operationId={}", request.getOperationId());
        otpService.validateCode(request.getOperationId(), request.getCode());
        return ResponseEntity.ok("OTP validated successfully");
    }
}