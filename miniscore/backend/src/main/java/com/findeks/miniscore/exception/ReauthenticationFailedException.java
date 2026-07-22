package com.findeks.miniscore.exception;

/**
 * Hassas bir islem (rol degisimi) icin istenen sifre tekrar dogrulamasi basarisiz.
 * GlobalExceptionHandler bunu 403'e cevirir.
 *
 * BILEREK 401 DEGIL: 401 istemcideki axios interceptor'ini tetikler (token yenile/oturum
 * kapat). Oysa burada oturum gecerli; sadece bu ISLEM icin sifre yanlis. 403 ile sayfa
 * hatayi kendisi gosterir, oturuma dokunulmaz.
 */
public class ReauthenticationFailedException extends RuntimeException {
    public ReauthenticationFailedException(String message) {
        super(message);
    }
}
