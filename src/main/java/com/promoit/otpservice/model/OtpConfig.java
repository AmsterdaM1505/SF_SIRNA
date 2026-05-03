package com.promoit.otpservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "otp_config")
@Data
public class OtpConfig {
    @Id
    private Integer id = 1;

    @Column(name = "code_length", nullable = false)
    private Integer codeLength = 6;

    @Column(name = "lifetime_seconds", nullable = false)
    private Integer lifetimeSeconds = 300;
}