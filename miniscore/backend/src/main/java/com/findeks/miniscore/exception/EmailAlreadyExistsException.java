package com.findeks.miniscore.exception;

// Ayri bir tip: "bu bir cakisma (409)" bilgisini TIP tasiyor, metin degil.
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
