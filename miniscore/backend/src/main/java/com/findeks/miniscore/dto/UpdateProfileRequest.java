package com.findeks.miniscore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * PUT /api/users/me isteğinin gövdesi.
 *
 * SADECE ad ve soyad guncellenebilir. e-posta ve tcNo BILEREK disarida:
 *  - e-posta kullanicinin GIRIS KIMLIGI (JWT'nin sub'i). Degisirse elindeki token'in
 *    sub'i eski kalir -> JwtAuthFilter kullaniciyi bulamaz -> beklenmedik 401'ler.
 *    (User.java'daki @PreUpdate notu tam bu tuzagi anlatiyor.)
 *  - tcNo skorun deterministik cekirdegi; degismesi tum skor gecmisinin anlamini bozar.
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Ad boş olamaz")
    private String firstName;

    @NotBlank(message = "Soyad boş olamaz")
    private String lastName;
}
