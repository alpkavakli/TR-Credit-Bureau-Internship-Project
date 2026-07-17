package com.findeks.miniscore.common;

import java.util.Locale;

/**
 * E-posta normalizasyon kurali - TEK KAYNAK.
 *
 * Neden ayri bir sinif?
 * Once bu kural iki yere kopyalanmisti: AuthService.normalize() ve User.@PrePersist.
 * Sonra AdminSeeder'a da lazim oldu ve orada UNUTULDU -> seeder'in idempotentligi
 * bozuldu (yapilandirilan e-posta buyuk harfliyse her acilista tekrar insert denemesi).
 * Ders: ayni kural birden fazla yerde tekrarlaninca, er ya da gec biri geride kalir.
 * Artik kural tek yerde; cagiran herkes ayni sonucu alir.
 *
 * final class + private constructor: bu sinif "utility" -> ondan nesne uretilmesi
 * ya da miras alinmasi anlamsiz, o yuzden dilin kendisiyle engelliyoruz.
 */
public final class EmailUtils {

    private EmailUtils() {
        // ornegi olusturulamaz
    }

    /**
     * E-postayi karsilastirilabilir tek bicime cevirir: bosluksuz + kucuk harf.
     *
     * Locale.ROOT SART: Turkce locale'de "I".toLowerCase() -> "ı" olur ve
     * e-postayi bozardi (ALPER@X.COM -> alper@x.com degil, "ılper@x.com").
     *
     * null girdi null doner -> cagiran taraf @NotBlank/@Valid ile zaten
     * null'i engelliyor; burada patlatmak yerine gecirgen davraniyoruz.
     */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
