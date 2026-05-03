package com.promoit.otpservice.scheduler;

import com.promoit.otpservice.repository.OtpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OtpExpirationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OtpExpirationScheduler.class);
    private final OtpCodeRepository codeRepository;

    public OtpExpirationScheduler(OtpCodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    @Scheduled(fixedDelay = 30000)
    public void expireOldCodes() {
        int updated = codeRepository.expireOldCodes(LocalDateTime.now());
        if (updated > 0) {
            logger.info("Expired {} OTP codes", updated);
        }
    }
}