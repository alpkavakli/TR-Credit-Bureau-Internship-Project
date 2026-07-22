package com.findeks.miniscore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * POST /api/auth/verify isteğinin gövdesi: hangi e-posta ve hangi kod.
 */
@Data
public class VerifyRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank(message = "Kod boş olamaz")
    private String code;
}
