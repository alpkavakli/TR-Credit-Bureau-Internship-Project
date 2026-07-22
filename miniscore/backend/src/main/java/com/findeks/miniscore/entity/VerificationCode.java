package com.findeks.miniscore.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * E-posta 2FA için tek kullanımlık doğrulama kodu.
 *
 * Kod DÜZ metin saklanmaz, BCrypt hash'i saklanır (DB sızsa bile kod okunamaz).
 * Bir e-posta için aynı anda tek aktif kod tutulur (yenisi eskisini siler).
 */
@Entity
@Table(name = "verification_codes")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    // Kaba kuvvet denemesini sınırlamak için: belirli sayıda yanlış deneme sonrası kod ölür.
    @Column(nullable = false)
    private int attempts;
}
