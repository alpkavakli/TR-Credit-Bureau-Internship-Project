package com.findeks.miniscore.exception;

/**
 * Doğrulama kodu yanlış / süresi dolmuş / hiç istenmemiş.
 * GlobalExceptionHandler bunu 400'e çevirir.
 *
 * BİLEREK 401 DEĞİL: /api/auth/verify çağrısında kullanıcının henüz token'ı yoktur;
 * 401 dönersek istemcideki interceptor "oturum bitti" sanıp login'e atardı. 400 ile
 * doğrulama ekranı hatayı kendisi gösterir, kullanıcı tekrar dener.
 */
public class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}
