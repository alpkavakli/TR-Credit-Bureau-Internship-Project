package com.findeks.miniscore.controller;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.findeks.miniscore.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
 
   private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
   
   
   @ExceptionHandler(RuntimeException.class)
   public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Hata", ex); //loglama ekledim
       return ResponseEntity.badRequest()
               .body(new ErrorResponse(ex.getMessage()));
   }
 
  @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<ErrorResponse> handleValidation(
           MethodArgumentNotValidException ex) {
       String message = ex.getBindingResult().getFieldErrors().stream()
               .map(e -> e.getField() + ": " + e.getDefaultMessage())
              .collect(Collectors.joining(", "));
       return ResponseEntity.badRequest()
               .body(new ErrorResponse(message));
   }
}

/*@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("E-posta veya şifre hatalı."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Kimlik doğrulama başarısız."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Bu işlem için yetkiniz yok."));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // unchanged
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Beklenmeyen hata", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Sunucu hatası."));
    }
}
 */