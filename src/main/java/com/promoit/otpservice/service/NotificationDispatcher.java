package com.promoit.otpservice.service;

import com.promoit.otpservice.exception.ServiceException;
import com.promoit.otpservice.service.impl.EmailNotificationService;
import com.promoit.otpservice.service.impl.FileNotificationService;
import com.promoit.otpservice.service.impl.SmsNotificationService;
import com.promoit.otpservice.service.impl.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class NotificationDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final SmsNotificationService smsService;
    private final EmailNotificationService emailService;
    private final TelegramNotificationService telegramService;
    private final FileNotificationService fileService;

    public NotificationDispatcher(SmsNotificationService smsService,
                                  EmailNotificationService emailService,
                                  TelegramNotificationService telegramService,
                                  FileNotificationService fileService) {
        this.smsService = smsService;
        this.emailService = emailService;
        this.telegramService = telegramService;
        this.fileService = fileService;
    }

    public void dispatch(Set<String> channels, String email, String phone, String telegramChatId, String code, String username) {
        for (String channel : channels) {
            try {
                switch (channel.toUpperCase()) {
                    case "SMS":
                        smsService.sendCode(phone, code);
                        logger.info("OTP sent via SMS to {}", phone);
                        break;
                    case "EMAIL":
                        emailService.sendCode(email, code);
                        logger.info("OTP sent via Email to {}", email);
                        break;
                    case "TELEGRAM":
                        telegramService.sendCode(telegramChatId, code);
                        logger.info("OTP sent via Telegram");
                        break;
                    case "FILE":
                        fileService.sendCode(username, code);
                        logger.info("OTP saved to file for user {}", username);
                        break;
                    default:
                        throw new ServiceException("Unsupported channel: " + channel);
                }
            } catch (Exception e) {
                logger.error("Failed to send OTP via {}: {}", channel, e.getMessage());
            }
        }
    }
}