package com.findeks.miniscore.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.findeks.miniscore.dto.AuthResponse;
import com.findeks.miniscore.dto.LoginRequest;
import com.findeks.miniscore.dto.RegisterRequest;
import com.findeks.miniscore.entity.Role;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.repository.UserRepository;
import com.findeks.miniscore.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class AuthService {
 
   private final UserRepository userRepository;
   private final PasswordEncoder passwordEncoder;
   private final JwtService jwtService;
   private final AuthenticationManager authManager;
 
   public AuthResponse register(RegisterRequest request) {
       if (userRepository.existsByEmail(request.getEmail()))
           throw new RuntimeException("Bu e-posta adresi zaten kayıtlıdır.");
 
       User user = User.builder()
              .firstName(request.getFirstName())
              .lastName(request.getLastName())
               .tcNo(request.getTcNo())
               .email(request.getEmail())
              .password(passwordEncoder.encode(request.getPassword()))
               .role(Role.USER)
               .build();
       userRepository.save(user);
 
       String token = jwtService.generateToken(user);
       return new AuthResponse(token, user.getEmail(), user.getRole().name());
   }
 
   public AuthResponse login(LoginRequest request) {
       authManager.authenticate(new UsernamePasswordAuthenticationToken(
               request.getEmail(), request.getPassword()));
       User user = userRepository.findByEmail(request.getEmail())
               .orElseThrow();
       String token = jwtService.generateToken(user);
       return new AuthResponse(token, user.getEmail(), user.getRole().name());
   }
}