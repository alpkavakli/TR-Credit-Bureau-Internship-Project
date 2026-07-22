package com.findeks.miniscore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;          // kisa omurlu access JWT
    private String refreshToken;   // uzun omurlu, DB'de tutulan opak yenileme anahtari
    private String email;
    private String role;
}
