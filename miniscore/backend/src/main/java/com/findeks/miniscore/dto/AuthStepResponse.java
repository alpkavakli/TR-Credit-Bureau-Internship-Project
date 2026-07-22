package com.findeks.miniscore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * login/register yanıtı. İki olası durum:
 *  - verificationRequired = false -> auth doludur (token + refresh), akış biter.
 *  - verificationRequired = true  -> auth null, email dolu. İstemci kodu /auth/verify'a
 *    göndererek asıl token'ları alır (e-posta 2FA açık ve kullanıcı ADMIN değilse).
 */
@Data
@AllArgsConstructor
public class AuthStepResponse {
    private boolean verificationRequired;
    private String email;
    private AuthResponse auth;

    public static AuthStepResponse completed(AuthResponse auth) {
        return new AuthStepResponse(false, auth.getEmail(), auth);
    }

    public static AuthStepResponse pending(String email) {
        return new AuthStepResponse(true, email, null);
    }
}
