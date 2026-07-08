package com.findeks.miniscore.controller;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.findeks.miniscore.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
 
   @ExceptionHandler(RuntimeException.class)
   public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
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