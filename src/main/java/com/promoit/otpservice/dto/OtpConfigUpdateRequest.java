package com.promoit.otpservice.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OtpConfigUpdateRequest {
    @Min(4)
    private Integer codeLength;
    @Min(30)
    private Integer lifetimeSeconds;
}