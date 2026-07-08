package com.findeks.miniscore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.findeks.miniscore.dto.AuthResponse;
import com.findeks.miniscore.dto.LoginRequest;
import com.findeks.miniscore.dto.RegisterRequest;
import com.findeks.miniscore.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
 
   private final AuthService authService;

 
   @PostMapping("/register")
   public ResponseEntity<AuthResponse> register(
           @Valid @RequestBody RegisterRequest request) {
       return ResponseEntity.status(201).body(authService.register(request));
   }
 
   @PostMapping("/login")
   public ResponseEntity<AuthResponse> login(
           @Valid @RequestBody LoginRequest request) {
       return ResponseEntity.ok(authService.login(request));
   }
}