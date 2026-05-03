package com.promoit.otpservice;

import com.promoit.otpservice.dto.OtpConfigUpdateRequest;
import com.promoit.otpservice.exception.ServiceException;
import com.promoit.otpservice.model.OtpCode;
import com.promoit.otpservice.model.OtpConfig;
import com.promoit.otpservice.model.User;
import com.promoit.otpservice.repository.OtpCodeRepository;
import com.promoit.otpservice.repository.OtpConfigRepository;
import com.promoit.otpservice.repository.UserRepository;
import com.promoit.otpservice.service.NotificationDispatcher;
import com.promoit.otpservice.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpConfigRepository configRepository;
    @Mock
    private OtpCodeRepository codeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationDispatcher dispatcher;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(configRepository, codeRepository, userRepository, dispatcher);
    }

    @Test
    void generateAndSend_shouldSaveAndDispatche() {
        User user = new User();
        user.setId(1L);
        user.setUsername("user1");
        OtpConfig config = new OtpConfig();
        config.setCodeLength(6);
        config.setLifetimeSeconds(300);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(configRepository.findById(1)).thenReturn(Optional.of(config));
        when(codeRepository.findByOperationIdAndStatus("op1", OtpCode.OtpStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(codeRepository.save(any(OtpCode.class))).thenReturn(new OtpCode());

        otpService.generateAndSend(1L, "op1",
                Set.of("SMS", "FILE"), "", "1234567890", "");

        verify(codeRepository).save(any(OtpCode.class));
        verify(dispatcher).dispatch(anySet(), anyString(), anyString(), anyString(), anyString(), eq("user1"));
    }

    @Test
    void generateAndSend_shouldThrowIfActiveCodeExists() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(codeRepository.findByOperationIdAndStatus("op1", OtpCode.OtpStatus.ACTIVE))
                .thenReturn(Optional.of(new OtpCode()));

        assertThrows(ServiceException.class, () ->
                otpService.generateAndSend(1L, "op1", Set.of("SMS"), "", "", ""));
    }

    @Test
    void validateCode_shouldMarkUsed() {
        OtpCode otpCode = new OtpCode();
        otpCode.setCode("123456");
        otpCode.setStatus(OtpCode.OtpStatus.ACTIVE);
        otpCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(codeRepository.findByOperationIdAndStatus("op1", OtpCode.OtpStatus.ACTIVE))
                .thenReturn(Optional.of(otpCode));

        assertTrue(otpService.validateCode("op1", "123456"));
        assertEquals(OtpCode.OtpStatus.USED, otpCode.getStatus());
        verify(codeRepository).save(otpCode);
    }

    @Test
    void validateCode_shouldThrowIfExpired() {
        OtpCode otpCode = new OtpCode();
        otpCode.setCode("123456");
        otpCode.setStatus(OtpCode.OtpStatus.ACTIVE);
        otpCode.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        when(codeRepository.findByOperationIdAndStatus("op1", OtpCode.OtpStatus.ACTIVE))
                .thenReturn(Optional.of(otpCode));

        assertThrows(ServiceException.class, () -> otpService.validateCode("op1", "123456"));
        assertEquals(OtpCode.OtpStatus.EXPIRED, otpCode.getStatus());
    }

    @Test
    void updateConfig_shouldChangeValues() {
        OtpConfig config = new OtpConfig();
        config.setCodeLength(6);
        config.setLifetimeSeconds(300);
        when(configRepository.findById(1)).thenReturn(Optional.of(config));
        when(configRepository.save(any(OtpConfig.class))).thenReturn(config);

        OtpConfigUpdateRequest req = new OtpConfigUpdateRequest();
        req.setCodeLength(8);
        req.setLifetimeSeconds(600);
        OtpConfig updated = otpService.updateConfig(req);

        assertEquals(8, updated.getCodeLength());
        assertEquals(600, updated.getLifetimeSeconds());
    }
}