package com.findeks.miniscore.exception;

/**
 * Refresh token bulunamadi / suresi dolmus / iptal edilmis.
 * GlobalExceptionHandler bunu 401'e cevirir -> istemci kullaniciyi login'e yollar.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
