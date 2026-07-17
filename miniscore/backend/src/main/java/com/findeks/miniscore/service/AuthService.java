package com.findeks.miniscore.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.findeks.miniscore.common.EmailUtils;
import com.findeks.miniscore.dto.AuthResponse;
import com.findeks.miniscore.dto.LoginRequest;
import com.findeks.miniscore.dto.RegisterRequest;
import com.findeks.miniscore.entity.Role;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.exception.EmailAlreadyExistsException;
import com.findeks.miniscore.exception.NotFoundException;
import com.findeks.miniscore.repository.UserRepository;
import com.findeks.miniscore.security.JwtService;

import lombok.RequiredArgsConstructor;


@Service @RequiredArgsConstructor
public class AuthService {

   private final UserRepository userRepository;
   private final PasswordEncoder passwordEncoder;
   private final JwtService jwtService;
   private final AuthenticationManager authManager;
   private final AuditService auditService;   // denetim kaydi icin ortak servis

   // @Transactional: kullanici kaydi tek transaction icinde olsun (ya tamami ya hicbiri).
   // Audit REQUIRES_NEW oldugu icin bu transaction'a BAGLI DEGIL -> rollback olsa bile iz kalir.
   @Transactional
   public AuthResponse register(RegisterRequest request) {
       // E-postayi ONCE normalize ediyoruz. User'daki @PrePersist zaten DB'ye kucuk
       // harfle yaziyor; ama existsByEmail SORGUSU da ayni formatta olmali, yoksa
       // "Alp@Test.com" ile arayip mevcut "alp@test.com" kaydini bulamazdik.
       String email = normalize(request.getEmail());

       if (userRepository.existsByEmail(email))
             // Ciplak RuntimeException degil -> TIP'in kendisi "bu 409'dur" bilgisini tasiyor.
             throw new EmailAlreadyExistsException("Bu e-posta adresi zaten kayıtlıdır.");

       User user = User.builder()
              .firstName(request.getFirstName())
              .lastName(request.getLastName())
               .tcNo(request.getTcNo())
               .email(email)
              .password(passwordEncoder.encode(request.getPassword()))   // duz sifre ASLA saklanmaz
               .role(Role.USER)   // herkes USER dogar; ADMIN'i sadece seeder ya da baska bir admin verir
               .build();
       userRepository.save(user);

       // Yeni hesap acilmasi denetlenmesi gereken bir olaydir.
       auditService.log("REGISTER", email,
               "Yeni kullanıcı kaydı: " + user.getFirstName() + " " + user.getLastName());

       String token = jwtService.generateToken(user);
       return new AuthResponse(token, user.getEmail(), user.getRole().name());
   }

     public AuthResponse login(LoginRequest request) {
       String email = normalize(request.getEmail());

       try {
           // authenticate(): CustomUserDetailsService ile kullaniciyi bulur, PasswordEncoder ile
           // sifreyi karsilastirir. Uyusmazsa BadCredentialsException firlatir.
           authManager.authenticate(new UsernamePasswordAuthenticationToken(
                   email, request.getPassword()));
       } catch (BadCredentialsException ex) {
           // BASARISIZ girisi de logluyoruz -> denetim acisindan en degerli kayit budur
           // (arka arkaya 50 basarisiz LOGIN = saldiri sinyali).
           auditService.log("LOGIN_FAILED", email, "Hatalı e-posta veya şifre");
           throw ex;   // exception'i YUTMUYORUZ; GlobalExceptionHandler bunu 401'e cevirecek
       }

       User user = userRepository.findByEmail(email)
               // Ciplak orElseThrow() NoSuchElementException firlatirdi -> mesajsiz.
               // authManager zaten dogruladigi icin buraya normalde DUSMEZ;
               // dusuyorsa veri tutarsizligi vardir, o yuzden 404 makul.
               .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));

       auditService.log("LOGIN", email, "Başarılı giriş");

       String token = jwtService.generateToken(user);
       return new AuthResponse(token, user.getEmail(), user.getRole().name());
   }

   // Kural burada DEGIL, EmailUtils'te duruyor -> User.@PrePersist ve AdminSeeder de
   // ayni metodu cagiriyor. Once bu metot burada kopyalanmisti ve AdminSeeder'a
   // eklenmesi unutulunca seeder'in idempotentligi bozuldu; o yuzden tek yere tasindi.
   private String normalize(String email) {
       return EmailUtils.normalize(email);
   }
}
