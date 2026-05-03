package com.promoit.otpservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpValidateRequest {
    @NotBlank
    private String operationId;
    @NotBlank
    private String code;
}