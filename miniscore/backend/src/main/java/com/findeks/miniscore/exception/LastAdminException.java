package com.findeks.miniscore.exception;

/**
 * Sistemdeki SON admin'in yetkisi elinden alinmak istendiginde firlar.
 *
 * Neden 409 (Conflict)?
 *  - 400 degil: istek bicimsel olarak kusursuz, "ADMIN"/"USER" gecerli degerler.
 *  - 403 degil: cagiran kisinin yetkisi var, sorun yetkide degil.
 *  - 409: istek sistemin MEVCUT DURUMU ile cakisiyor ("tek admin sensin").
 *    Ayni EmailAlreadyExistsException gibi -> "gecerli istek, imkansiz durum".
 */
public class LastAdminException extends RuntimeException {

    public LastAdminException(String message) {
        super(message);
    }
}
