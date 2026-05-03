package com.promoit.otpservice.service;

import com.promoit.otpservice.dto.UserResponse;
import com.promoit.otpservice.exception.ServiceException;
import com.promoit.otpservice.model.User;
import com.promoit.otpservice.repository.OtpCodeRepository;
import com.promoit.otpservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;

    public UserService(UserRepository userRepository, OtpCodeRepository otpCodeRepository) {
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
    }

    public List<UserResponse> getAllNonAdminUsers() {
        logger.info("Fetching all non-admin users");
        return userRepository.findAllByRoleNot("ADMIN").stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Long userId) {
        logger.info("Deleting user with id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException("User not found"));
        if ("ADMIN".equals(user.getRole())) {
            throw new ServiceException("Cannot delete admin user");
        }
        // Удаляем все OTP коды пользователя
        otpCodeRepository.deleteAllByUser(user);
        userRepository.delete(user);
        logger.info("User {} and associated OTP codes deleted", userId);
    }
}