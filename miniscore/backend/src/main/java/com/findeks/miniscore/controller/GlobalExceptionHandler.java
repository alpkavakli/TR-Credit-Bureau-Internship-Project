package com.findeks.miniscore.controller;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;   // govde JSON'a cevrilemedi
import org.springframework.security.access.AccessDeniedException;          // method security (@PreAuthorize) reddi
import org.springframework.security.authentication.BadCredentialsException; // sifre/email hatali
import org.springframework.security.core.AuthenticationException;          // diger tum kimlik dogrulama hatalari
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.findeks.miniscore.dto.ErrorResponse;
import com.findeks.miniscore.exception.EmailAlreadyExistsException;
import com.findeks.miniscore.exception.LastAdminException;
import com.findeks.miniscore.exception.NotFoundException;

// @RestControllerAdvice = @ControllerAdvice + @ResponseBody
// Tum controller'lardan sizan exception'lari yakalar, donusu JSON'a cevirir.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 401 - sifre yanlis. BadCredentialsException, AuthenticationException'in alt tipi;
    // Spring EN SPESIFIK handler'i sectigi icin asagidaki genel handler'i ezer.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        // DIKKAT: "email yok" ile "sifre yanlis" ayrimini KASTEN yapmiyoruz.
        // Ayirirsak saldirgan hangi e-postalarin kayitli oldugunu ogrenir (user enumeration).
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("E-posta veya şifre hatalı."));
    }

    // 401 - kimlik dogrulamanin diger tum hatalari (hesap kilitli, devre disi vb.)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Kimlik doğrulama başarısız."));
    }

    // 403 - kimligin belli AMA yetkin yok. @PreAuthorize buraya duser.
    // Bu handler OLMAZSA asagidaki Exception handler'i yakalar ve 403 yerine 500 doner.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Bu işlem için yetkiniz yok."));
    }

    // 409 Conflict - istek gecerli ama kaynagin mevcut durumuyla cakisiyor.
    // 400 degil: istekte bir yazim hatasi yok, sadece o e-posta zaten alinmis.
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // 409 Conflict - is kurali geregi imkansiz durum: son admin'in yetkisi alinamaz.
    // EmailAlreadyExists ile ayni mantik: istek bicimsel olarak gecerli (400 degil),
    // cagiranin yetkisi var (403 degil), ama sistemin MEVCUT DURUMU izin vermiyor.
    @ExceptionHandler(LastAdminException.class)
    public ResponseEntity<ErrorResponse> handleLastAdmin(LastAdminException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // 404 - istenen kayit yok.
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // 400 - @Valid dogrulamasi patladi. Kullaniciya HANGI alan neden gecersiz onu soyluyoruz.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(message));
    }

    // 400 - govdedeki JSON hic okunamadi.
    // Ornek: {"role":"MUDUR"} -> Role enum'unda MUDUR yok; ya da bozuk/eksik JSON.
    // Bu hata @Valid'den ONCE olur (Jackson daha nesneyi kuramaz), o yuzden
    // MethodArgumentNotValidException DEGIL, ayri bir exception firlar.
    // Bu handler olmasaydi asagidaki Exception'a duser ve istemcinin hatasi 500 gorunurdu.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        // ex.getMessage()'i istemciye VERMIYORUZ: icinde paket/sinif adlarimiz geciyor.
        log.warn("Okunamayan istek gövdesi: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("İstek gövdesi geçersiz. Alan tiplerini kontrol edin."));
    }

    // 500 - son savunma hatti. Buraya dusen her sey BIZIM bugumuzdur.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Stack trace SADECE log'a. ex.getMessage()'i istemciye VERMIYORUZ:
        // icinde SQL, tablo adi, dosya yolu gibi bilgi sizabilir.
        log.error("Beklenmeyen hata", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Sunucu hatası."));
    }
}
