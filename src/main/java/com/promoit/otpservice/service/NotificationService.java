package com.promoit.otpservice.service;

public interface NotificationService {
    void sendCode(String recipient, String code);
}