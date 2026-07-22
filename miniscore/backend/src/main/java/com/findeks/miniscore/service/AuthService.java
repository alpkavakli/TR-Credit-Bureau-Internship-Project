package com.findeks.miniscore.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.findeks.miniscore.common.EmailUtils;
import com.findeks.miniscore.dto.AuthResponse;
import com.findeks.miniscore.dto.AuthStepResponse;
import com.findeks.miniscore.dto.LoginRequest;
import com.findeks.miniscore.dto.RegisterRequest;
import com.findeks.miniscore.entity.RefreshToken;
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
   private final RefreshTokenService refreshTokenService;   // uzun omurlu yenileme anahtari
   private final SettingService settingService;             // e-posta 2FA acik mi?
   private final VerificationService verificationService;   // kod uret/gonder/dogrula

   // @Transactional: kullanici kaydi tek transaction icinde olsun (ya tamami ya hicbiri).
   // Audit REQUIRES_NEW oldugu icin bu transaction'a BAGLI DEGIL -> rollback olsa bile iz kalir.
   @Transactional
   public AuthStepResponse register(RegisterRequest request) {
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

       // 2FA acik ve kullanici ADMIN degilse: token verme, once e-posta kodu iste.
       if (twoFactorRequiredFor(user)) {
           verificationService.createAndSend(user.getEmail());
           return AuthStepResponse.pending(user.getEmail());
       }
       return AuthStepResponse.completed(issueTokens(user));
   }

     public AuthStepResponse login(LoginRequest request) {
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

       // Sifre dogru; ama 2FA acik ve kullanici ADMIN degilse token'i HEMEN vermiyoruz.
       if (twoFactorRequiredFor(user)) {
           verificationService.createAndSend(user.getEmail());
           auditService.log("LOGIN_2FA_SENT", email, "Doğrulama kodu gönderildi");
           return AuthStepResponse.pending(user.getEmail());
       }

       auditService.log("LOGIN", email, "Başarılı giriş");
       return AuthStepResponse.completed(issueTokens(user));
   }

   /**
    * İki adımlı doğrulamanın SON adımı: e-postaya giden kod doğrulanır ve asıl token'lar
    * verilir. Kod yanlış/süresi dolmuşsa VerificationService hata fırlatır (400).
    */
   @Transactional
   public AuthResponse verify(String rawEmail, String code) {
       String email = normalize(rawEmail);
       User user = userRepository.findByEmail(email)
               .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));

       verificationService.verify(email, code);

       auditService.log("LOGIN", email, "Başarılı giriş (2FA)");
       return issueTokens(user);
   }

   /**
    * Access token suresi dolunca istemci bu ucu cagirir: gecerli bir refresh token
    * verir, karsiliginda YENI access + YENI refresh token alir (rotation).
    * Refresh token gecersiz/suresi dolmussa RefreshTokenService 401 firlatir.
    */
   @Transactional
   public AuthResponse refresh(String refreshToken) {
       RefreshToken rotated = refreshTokenService.verifyAndRotate(refreshToken);
       User user = userRepository.findByEmail(rotated.getUserEmail())
               .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));
       String token = jwtService.generateToken(user);
       return new AuthResponse(token, rotated.getToken(), user.getEmail(), user.getRole().name());
   }

   // Access + refresh token uretip AuthResponse doner (2FA'siz akis ve verify sonrasi ortak).
   private AuthResponse issueTokens(User user) {
       String token = jwtService.generateToken(user);
       RefreshToken refresh = refreshTokenService.create(user.getEmail());
       return new AuthResponse(token, refresh.getToken(), user.getEmail(), user.getRole().name());
   }

   // 2FA ayari acik VE kullanici ADMIN degilse e-posta dogrulamasi gerekir.
   // ADMIN muaf: seeded admin'in e-postasi (admin@findeks.com) gercek degil; 2FA acilinca
   // admin kod alamayip kilitlenir ve ayari kapatmak icin panele giremezdi.
   private boolean twoFactorRequiredFor(User user) {
       return settingService.isEmailTwoFactorEnabled() && user.getRole() != Role.ADMIN;
   }

   // Kural burada DEGIL, EmailUtils'te duruyor -> User.@PrePersist ve AdminSeeder de
   // ayni metodu cagiriyor. Once bu metot burada kopyalanmisti ve AdminSeeder'a
   // eklenmesi unutulunca seeder'in idempotentligi bozuldu; o yuzden tek yere tasindi.
   private String normalize(String email) {
       return EmailUtils.normalize(email);
   }
}
