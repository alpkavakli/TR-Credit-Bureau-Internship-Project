package com.findeks.miniscore.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.findeks.miniscore.dto.UpdateProfileRequest;
import com.findeks.miniscore.dto.UserResponse;
import com.findeks.miniscore.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Giris yapmis kullanicinin KENDI profili. "/me" -> kim oldugunu JWT'den (currentUser)
 * aliyoruz, URL'de id tasimiyoruz; boylece bir kullanici baskasinin profiline erisemez.
 * SecurityConfig'te "anyRequest().authenticated()" bu ucu zaten token zorunlu kilar.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(
            @AuthenticationPrincipal UserDetails currentUser) {
        return ResponseEntity.ok(userService.getByEmail(currentUser.getUsername()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(
                currentUser.getUsername(), request.getFirstName(), request.getLastName()));
    }
}
