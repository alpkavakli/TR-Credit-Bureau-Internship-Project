package com.findeks.miniscore.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.findeks.miniscore.entity.VerificationCode;
import com.findeks.miniscore.exception.InvalidVerificationCodeException;
import com.findeks.miniscore.repository.VerificationCodeRepository;

import lombok.RequiredArgsConstructor;

/**
 * E-posta 2FA kodlarının üretimi, gönderimi ve doğrulanması.
 *
 * Kod DÜZ metin saklanmaz (BCrypt hash). Bir e-posta için tek aktif kod tutulur.
 * Yanlış denemeler sayılır; sınır aşılınca kod ölür (kaba kuvvet koruması).
 */
@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.verification.code-length}")
    private int codeLength;

    @Value("${app.verification.expiration-minutes}")
    private long expirationMinutes;

    @Value("${app.verification.max-attempts}")
    private int maxAttempts;

    /** Yeni kod üret, hash'leyip sakla, e-posta ile yolla. Eski kodu (varsa) siler. */
    @Transactional
    public void createAndSend(String email) {
        String code = randomNumericCode();

        verificationCodeRepository.deleteByEmail(email);   // eski kodu tüket
        verificationCodeRepository.flush();                // aynı e-posta için unique çakışmasını önle
        verificationCodeRepository.save(VerificationCode.builder()
                .email(email)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES))
                .attempts(0)
                .build());

        emailService.sendVerificationCode(email, code);
    }

    /** Kodu doğrula. Başarılıysa kodu siler; değilse uygun mesajla hata fırlatır. */
    @Transactional
    public void verify(String email, String code) {
        VerificationCode vc = verificationCodeRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidVerificationCodeException(
                        "Doğrulama kodu bulunamadı. Lütfen tekrar giriş yapın."));

        if (vc.getExpiresAt().isBefore(Instant.now())) {
            verificationCodeRepository.delete(vc);
            throw new InvalidVerificationCodeException(
                    "Doğrulama kodunun süresi doldu. Lütfen tekrar giriş yapın.");
        }

        if (vc.getAttempts() >= maxAttempts) {
            verificationCodeRepository.delete(vc);
            throw new InvalidVerificationCodeException(
                    "Çok fazla hatalı deneme. Lütfen tekrar giriş yapın.");
        }

        if (!passwordEncoder.matches(code, vc.getCodeHash())) {
            vc.setAttempts(vc.getAttempts() + 1);   // dirty-checking ile UPDATE
            throw new InvalidVerificationCodeException("Doğrulama kodu hatalı.");
        }

        verificationCodeRepository.delete(vc);   // tek kullanımlık
    }

    private String randomNumericCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
