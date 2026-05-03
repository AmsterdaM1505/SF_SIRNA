package com.promoit.otpservice.repository;

import com.promoit.otpservice.model.OtpConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpConfigRepository extends JpaRepository<OtpConfig, Integer> {
}