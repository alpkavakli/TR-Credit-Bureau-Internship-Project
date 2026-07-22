package com.findeks.miniscore.exception;

/**
 * Bir kullanicinin findeks skoru olmasi beklenmiyor (ör. ADMIN).
 * GlobalExceptionHandler bunu 403'e cevirir.
 *
 * Neden ADMIN'in skoru yok? Skor tcNo'dan deterministik uretilir; admin gercek bir
 * musteri degil, sistemi yoneten hesap. "00000000000" gibi sahte bir tcNo'dan skor
 * uretmek anlamsiz olurdu. Admin baskalarinin skorunu GORUR ama kendisi TASIMAZ.
 */
public class ScoreNotAllowedException extends RuntimeException {
    public ScoreNotAllowedException(String message) {
        super(message);
    }
}
