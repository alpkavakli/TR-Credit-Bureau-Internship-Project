package com.findeks.miniscore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.findeks.miniscore.dto.AuthResponse;
import com.findeks.miniscore.dto.AuthStepResponse;
import com.findeks.miniscore.dto.LoginRequest;
import com.findeks.miniscore.dto.RefreshRequest;
import com.findeks.miniscore.dto.RegisterRequest;
import com.findeks.miniscore.dto.VerifyRequest;
import com.findeks.miniscore.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
 
   private final AuthService authService;

 
   // Yanit AuthStepResponse: 2FA kapaliysa token doludur; aciksa "verificationRequired".
   @PostMapping("/register")
   public ResponseEntity<AuthStepResponse> register(
           @Valid @RequestBody RegisterRequest request) {
       return ResponseEntity.status(201).body(authService.register(request));
   }

   @PostMapping("/login")
   public ResponseEntity<AuthStepResponse> login(
           @Valid @RequestBody LoginRequest request) {
       return ResponseEntity.ok(authService.login(request));
   }

   // 2FA'nin ikinci adimi: e-postaya giden kod dogrulanir, token'lar burada verilir.
   @PostMapping("/verify")
   public ResponseEntity<AuthResponse> verify(
           @Valid @RequestBody VerifyRequest request) {
       return ResponseEntity.ok(authService.verify(request.getEmail(), request.getCode()));
   }

   @PostMapping("/refresh")
   public ResponseEntity<AuthResponse> refresh(
           @Valid @RequestBody RefreshRequest request) {
       return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
   }
}