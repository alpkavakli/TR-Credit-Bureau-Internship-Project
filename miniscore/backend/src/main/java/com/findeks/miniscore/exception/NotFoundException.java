package com.findeks.miniscore.exception;

// RuntimeException'dan turetiyoruz (checked degil unchecked).
// Checked olsaydi her cagiran metotta try/catch veya "throws" yazmak zorunda kalirdik;
// Spring'in dunyasinda hata yonetimi merkezi oldugu icin unchecked tercih edilir.
public class NotFoundException extends RuntimeException {

    // super(message) -> mesaji RuntimeException'in kendi alanina yaziyor,
    // boylece handler icinde ex.getMessage() ile okuyabiliyoruz.
    public NotFoundException(String message) {
        super(message);
    }
}
