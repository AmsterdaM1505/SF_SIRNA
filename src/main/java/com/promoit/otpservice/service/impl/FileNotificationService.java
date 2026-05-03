package com.promoit.otpservice.service.impl;

import com.promoit.otpservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(FileNotificationService.class);
    private static final String FILE_PATH = "otp_codes.log"; // в корне проекта

    @Override
    public void sendCode(String username, String code) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String line = String.format("[%s] User: %s, OTP: %s", timestamp, username, code);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.write(line);
            writer.newLine();
            logger.info("OTP code saved to file: {}", FILE_PATH);
        } catch (IOException e) {
            logger.error("Failed to write OTP code to file", e);
            throw new RuntimeException("File write error", e);
        }
    }
}