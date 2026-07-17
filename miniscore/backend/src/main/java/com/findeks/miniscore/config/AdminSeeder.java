package com.findeks.miniscore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.findeks.miniscore.common.EmailUtils;
import com.findeks.miniscore.entity.Role;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Sistemde ilk ADMIN'i olusturur ("bootstrap" / "seeding" problemi).
 *
 * Problem: AuthService.register() HERKESI Role.USER yapiyor. Yani hicbir yoldan
 * ADMIN olunamiyordu -> AdminController'i test etmek imkansizdi.
 * Cozum secenekleri:
 *   1) register()'a "role" alani eklemek  -> FELAKET: herkes kendini admin yapardi.
 *   2) DB'ye elle SQL ile admin basmak    -> calisir ama her yeni ortamda elle is.
 *   3) Uygulama acilirken seeder          -> secilen bu. Sifre koddan degil YAML'den gelir.
 *
 * CommandLineRunner: Spring Boot uygulama tamamen ayaga kalktiktan HEMEN SONRA
 * run() metodunu bir kez calistirir. Bean'ler hazir oldugu icin repository kullanilabilir.
 */
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // @Value: application.yml'deki degeri alana enjekte eder.
    // ":" sonrasi VARSAYILAN deger -> yml'de yoksa uygulama patlamaz.
    @Value("${app.admin.email:admin@findeks.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin12345}")
    private String adminPassword;

    @Value("${app.admin.tc-no:00000000000}")
    private String adminTcNo;

    @Override
    public void run(String... args) {
        // E-postayi ONCE normalize ediyoruz - bu satir bir hatanin duzeltmesi:
        // Onceden existsByEmail(adminEmail) ham deger ile cagriliyordu. User'daki
        // @PrePersist ise DB'ye KUCUK HARFLE yaziyor. Yani yml'de "Admin@Findeks.com"
        // yazsaydik: 1. acilis -> bulunamaz -> insert (DB'ye kucuk harfli girer),
        // 2. acilis -> yine bulunamaz (aradigimiz buyuk harfli) -> tekrar insert ->
        // unique kisiti ihlali -> UYGULAMA HIC ACILMAZ.
        // Arama ile yazma AYNI bicimde olmali; kural artik EmailUtils'te tek yerde.
        String email = EmailUtils.normalize(adminEmail);

        // IDEMPOTENT olmali: uygulama her acilista calisir, ikinci kez admin YARATMAMALI.
        if (userRepository.existsByEmail(email)) {
            log.info("Admin kullanıcısı zaten mevcut: {}", email);
            return;
        }

        User admin = User.builder()
                .firstName("Sistem")
                .lastName("Yöneticisi")
                .tcNo(adminTcNo)
                .email(email)
                .password(passwordEncoder.encode(adminPassword))   // seeder'da bile duz sifre saklanmaz
                .role(Role.ADMIN)                                  // register()'in asla veremedigi rol
                .build();
        userRepository.save(admin);

        log.info("Admin kullanıcısı oluşturuldu: {}", adminEmail);
        // Sifreyi log'a YAZMIYORUZ. Gercek projede zaten sifre yml'de degil
        // ortam degiskeninde (environment variable) veya bir secret manager'da durur.
    }
}
