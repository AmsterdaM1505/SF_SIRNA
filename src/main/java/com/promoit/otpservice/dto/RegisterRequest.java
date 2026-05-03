package com.promoit.otpservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @Pattern(regexp = "ADMIN|USER", message = "Role must be ADMIN or USER")
    private String role = "USER";
}