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
 * Refresh token, kisa omurlu access JWT'yi yenilemek icin kullanilan uzun omurlu bir anahtar.
 *
 * NEDEN AYRI BIR TABLO / OPAK TOKEN?
 * Access token JWT'dir: sunucu onu DB'ye bakmadan, sadece imzasindan dogrular (stateless).
 * Bunun bedeli: bir JWT verildikten sonra suresi dolana kadar IPTAL EDILEMEZ.
 * Refresh token'i ise DB'de tutuyoruz (opak bir UUID, JWT degil) -> istedigimiz an
 * satiri silerek gecersiz kilabiliriz (cikis, sifre degisimi, calinma suphesi...).
 * Yani "kisa omurlu ama iptal edilemez JWT" ile "uzun omurlu ama iptal edilebilir kayit"
 * ikisini birlikte kullaniyoruz.
 */
@Entity
@Table(name = "refresh_tokens")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Rastgele UUID. JWT DEGIL: icinde bilgi tasimaz, sadece DB'deki satira isaret eder.
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    // User entity'sine @ManyToOne yerine sade e-posta: refresh akisi kullaniciyi
    // zaten e-postayla buluyor, lazy-load / dongusel referans derdi olmasin diye.
    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Instant expiresAt;
}
