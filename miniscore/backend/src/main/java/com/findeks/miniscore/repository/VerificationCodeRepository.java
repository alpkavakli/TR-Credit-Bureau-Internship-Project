package com.findeks.miniscore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.VerificationCode;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findByEmail(String email);

    // Yeni kod üretmeden önce eskisini temizlemek için (@Transactional metot içinden).
    void deleteByEmail(String email);
}
