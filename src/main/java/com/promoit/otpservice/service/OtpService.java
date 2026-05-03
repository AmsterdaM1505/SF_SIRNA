package com.promoit.otpservice.service;

import com.promoit.otpservice.dto.OtpConfigUpdateRequest;
import com.promoit.otpservice.exception.ServiceException;
import com.promoit.otpservice.model.OtpCode;
import com.promoit.otpservice.model.OtpConfig;
import com.promoit.otpservice.model.User;
import com.promoit.otpservice.repository.OtpCodeRepository;
import com.promoit.otpservice.repository.OtpConfigRepository;
import com.promoit.otpservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom random = new SecureRandom();

    private final OtpConfigRepository configRepository;
    private final OtpCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final NotificationDispatcher dispatcher;

    public OtpService(OtpConfigRepository configRepository,
                      OtpCodeRepository codeRepository,
                      UserRepository userRepository,
                      NotificationDispatcher dispatcher) {
        this.configRepository = configRepository;
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.dispatcher = dispatcher;
    }

    /**
     * Генерация OTP кода для операции, привязка к пользователю, сохранение и рассылка.
     */
    public void generateAndSend(Long userId, String operationId, Set<String> channels,
                                String email, String phone, String telegramChatId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found"));

        // Проверка, нет ли уже активного кода для операции
        Optional<OtpCode> existing = codeRepository.findByOperationIdAndStatus(operationId, OtpCode.OtpStatus.ACTIVE);
        if (existing.isPresent()) {
            throw new ServiceException("Active OTP already exists for operation: " + operationId);
        }

        OtpConfig config = getConfig();
        String code = generateCode(config.getCodeLength());
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(config.getLifetimeSeconds());

        OtpCode otpCode = new OtpCode();
        otpCode.setOperationId(operationId);
        otpCode.setCode(code);
        otpCode.setStatus(OtpCode.OtpStatus.ACTIVE);
        otpCode.setCreatedAt(LocalDateTime.now());
        otpCode.setExpiresAt(expiresAt);
        otpCode.setUser(user);

        codeRepository.save(otpCode);
        logger.info("Generated OTP code for operation {}: {}", operationId, code);

        // Рассылаем по каналам
        dispatcher.dispatch(channels, email, phone, telegramChatId, code, user.getUsername());
    }

    /**
     * Валидация OTP кода для операции.
     */
    @Transactional
    public boolean validateCode(String operationId, String inputCode) {
        OtpCode otpCode = codeRepository.findByOperationIdAndStatus(operationId, OtpCode.OtpStatus.ACTIVE)
                .orElseThrow(() -> new ServiceException("No active OTP for operation: " + operationId));

        if (LocalDateTime.now().isAfter(otpCode.getExpiresAt())) {
            otpCode.setStatus(OtpCode.OtpStatus.EXPIRED);
            codeRepository.save(otpCode);
            throw new ServiceException("OTP code expired");
        }

        if (!otpCode.getCode().equals(inputCode)) {
            throw new ServiceException("Invalid OTP code");
        }

        otpCode.setStatus(OtpCode.OtpStatus.USED);
        codeRepository.save(otpCode);
        logger.info("OTP code for operation {} validated and marked USED", operationId);
        return true;
    }

    /**
     * Изменение конфигурации OTP (администратор).
     */
    public OtpConfig updateConfig(OtpConfigUpdateRequest request) {
        OtpConfig config = getConfig();
        if (request.getCodeLength() != null) {
            config.setCodeLength(request.getCodeLength());
        }
        if (request.getLifetimeSeconds() != null) {
            config.setLifetimeSeconds(request.getLifetimeSeconds());
        }
        OtpConfig saved = configRepository.save(config);
        logger.info("OTP config updated: length={}, lifetime={}s", saved.getCodeLength(), saved.getLifetimeSeconds());
        return saved;
    }

    public OtpConfig getConfig() {
        return configRepository.findById(1)
                .orElseGet(() -> {
                    OtpConfig defaultConfig = new OtpConfig();
                    defaultConfig.setId(1);
                    defaultConfig.setCodeLength(6);
                    defaultConfig.setLifetimeSeconds(300);
                    configRepository.save(defaultConfig);
                    return defaultConfig;
                });
    }

    private String generateCode(int length) {
        int min = (int) Math.pow(10, length - 1);
        int max = (int) Math.pow(10, length) - 1;
        int code = min + random.nextInt(max - min + 1);
        return String.valueOf(code);
    }
}