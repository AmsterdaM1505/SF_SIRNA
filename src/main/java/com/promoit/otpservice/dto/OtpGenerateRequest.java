package com.promoit.otpservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class OtpGenerateRequest {
    @NotBlank
    private String operationId;
    @NotEmpty
    private Set<String> channels; // SMS, EMAIL, TELEGRAM, FILE
    private String email;
    private String phone;
    private String telegramChatId; // если не указан, берется из пропертей
}