package com.findeks.miniscore.exception;

/**
 * Bir admin KENDI rolunu degistiremez.
 * GlobalExceptionHandler bunu 409'a cevirir (LastAdmin ile ayni mantik: istek gecerli,
 * yetki var, ama is kurali bu islemi yasakliyor).
 *
 * Neden yasak? Admin kendini USER yaparsa aninda tum admin uclarina erisimini kaybeder;
 * geri donus icin baska bir admin ya da DB mudahalesi gerekir. Bu kaza cok kolay olur,
 * o yuzden kural seviyesinde engelliyoruz.
 */
public class SelfRoleChangeException extends RuntimeException {
    public SelfRoleChangeException(String message) {
        super(message);
    }
}
