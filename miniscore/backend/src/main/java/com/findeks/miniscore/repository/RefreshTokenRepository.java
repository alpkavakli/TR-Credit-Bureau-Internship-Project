package com.findeks.miniscore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // Derived delete: SELECT + DELETE. @Transactional bir metot icinden cagrilmali
    // (RefreshTokenService.deleteAllForUser bunu sagliyor).
    void deleteByUserEmail(String userEmail);
}
