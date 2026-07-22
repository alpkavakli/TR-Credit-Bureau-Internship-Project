package com.findeks.miniscore.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.findeks.miniscore.entity.RefreshToken;
import com.findeks.miniscore.exception.InvalidRefreshTokenException;
import com.findeks.miniscore.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Refresh token'larin uretimi, dogrulanmasi ve DONDURULMESI (rotation).
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken create(String userEmail) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userEmail(userEmail)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .build();
        return refreshTokenRepository.save(token);
    }

    /**
     * ROTATION: gecerli bir refresh token'i dogrular, ESKISINI SILER ve yenisini verir.
     * Boylece her refresh token yalnizca BIR kez kullanilabilir. Calinan bir token
     * mesru kullanici tarafindan bir kez kullanilinca eskir; hirsizin elindeki artik
     * gecersizdir (tek kullanimlik).
     */
    @Transactional
    public RefreshToken verifyAndRotate(String token) {
        RefreshToken existing = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "Oturum geçersiz. Lütfen tekrar giriş yapın."));

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(existing);
            throw new InvalidRefreshTokenException(
                    "Oturum süresi doldu. Lütfen tekrar giriş yapın.");
        }

        String userEmail = existing.getUserEmail();
        refreshTokenRepository.delete(existing);   // eskiyi tuket
        return create(userEmail);                  // yenisini ver
    }

    // Cikis / güvenlik olayinda kullanicinin tum refresh token'larini gecersiz kilar.
    @Transactional
    public void deleteAllForUser(String userEmail) {
        refreshTokenRepository.deleteByUserEmail(userEmail);
    }
}
