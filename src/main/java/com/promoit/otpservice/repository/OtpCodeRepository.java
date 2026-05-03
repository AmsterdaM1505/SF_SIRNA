package com.promoit.otpservice.repository;

import com.promoit.otpservice.model.OtpCode;
import com.promoit.otpservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findByOperationIdAndStatus(String operationId, OtpCode.OtpStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE OtpCode c SET c.status = 'EXPIRED' WHERE c.status = 'ACTIVE' AND c.expiresAt < :now")
    int expireOldCodes(LocalDateTime now);

    void deleteAllByUser(User user);
}