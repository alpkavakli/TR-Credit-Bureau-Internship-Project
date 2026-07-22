package com.findeks.miniscore.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.findeks.miniscore.dto.AuditLogResponse;
import com.findeks.miniscore.dto.CreditScoreResponse;
import com.findeks.miniscore.dto.SettingsResponse;
import com.findeks.miniscore.dto.UserResponse;
import com.findeks.miniscore.entity.AuditLog;
import com.findeks.miniscore.entity.Role;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.exception.LastAdminException;
import com.findeks.miniscore.exception.NotFoundException;
import com.findeks.miniscore.exception.ReauthenticationFailedException;
import com.findeks.miniscore.exception.ScoreNotAllowedException;
import com.findeks.miniscore.exception.SelfRoleChangeException;
import com.findeks.miniscore.repository.AuditLogRepository;
import com.findeks.miniscore.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

/**
 * Admin islemlerinin is mantigi.
 *
 * Neden controller dogrudan repository'ye dokunmuyor?
 * Katman kurali: controller = HTTP'yi bilir (durum kodu, JSON), service = isi bilir,
 * repository = DB'yi bilir. Controller repository'ye uzanirsa is mantigi HTTP katmanina
 * sizar ve mesela yarin ayni isi bir zamanlanmis job'dan cagirmak istedigimizde
 * kod tekrar yazilir. Ayrica @Transactional'in dogru yeri de burasidir.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final CreditScoreService creditScoreService;   // baska kullanicinin skor gecmisi icin
    private final PasswordEncoder passwordEncoder;         // rol degisiminde sifre tekrar dogrulamasi
    private final SettingService settingService;           // e-posta 2FA ayari

    // readOnly = true: sadece okuyoruz -> Hibernate "dirty checking" (degisiklik takibi)
    // yapmaz, gereksiz is yapmaz. Ayrica niyet bildirimi: bu metot veri DEGISTIRMEZ.
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)   // entity -> DTO (password disarida kalir)
                .collect(Collectors.toList());
    }

    /**
     * ADMIN, bir kullanicinin GUNCEL skorunu ve tum gecmisini gorur.
     * En yeni snapshot listenin sonundadir (weekIndex ASC). Admin'lerin raporu olmadigi
     * icin hedef ADMIN ise reddedilir. Kimin baktigini denetime yaziyoruz (KVKK/iz).
     */
    @Transactional
    public List<CreditScoreResponse> getUserScores(Long userId, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: id=" + userId));

        if (user.getRole() == Role.ADMIN) {
            throw new ScoreNotAllowedException("Yöneticilerin findeks raporu bulunmaz.");
        }

        List<CreditScoreResponse> history = creditScoreService.getHistoryForUser(user);
        auditService.log("ADMIN_SCORE_VIEW", adminEmail,
                "Kullanıcı skoru görüntülendi: " + user.getEmail());
        return history;
    }

    // ---- Sistem ayarları (e-posta 2FA açık/kapalı) ----

    @Transactional(readOnly = true)
    public SettingsResponse getSettings() {
        return new SettingsResponse(settingService.isEmailTwoFactorEnabled());
    }

    @Transactional
    public SettingsResponse setEmailTwoFactor(boolean enabled, String adminEmail) {
        settingService.setEmailTwoFactorEnabled(enabled);
        auditService.log("SETTING_CHANGE", adminEmail,
                "E-posta 2FA " + (enabled ? "açıldı" : "kapatıldı"));
        return new SettingsResponse(enabled);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: id=" + id));
        return toUserResponse(user);
    }

    // Page.map: sayfa metadata'sini (toplam, sayfa no, boyut) KORUYARAK icerigi
    // entity'den DTO'ya cevirir. Boylece frontend hem kayitlari hem "kac sayfa var"
    // bilgisini tek yanittan alir.
    // search bos ise tumu; doluysa performedBy (e-posta) uzerinden filtreli arama.
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAuditLogs(String search, Pageable pageable) {
        Page<AuditLog> page = (search == null || search.isBlank())
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : auditLogRepository.findByPerformedByContainingIgnoreCaseOrderByCreatedAtDesc(
                        search.trim(), pageable);
        return page.map(this::toAuditResponse);
    }

    /**
     * Bir kullanicinin rolunu degistirir.
     * readOnly DEGIL cunku yaziyoruz.
     *
     * setRole() cagirdiktan sonra save() YAZMAMIZA GEREK YOK: transaction icinde
     * okunan entity "managed" durumdadir, Hibernate transaction sonunda degisikligi
     * gorup UPDATE'i kendi atar (dirty checking). save() yazmak da yanlis olmazdi.
     */
    @Transactional
    public UserResponse updateRole(Long id, Role newRole, String performedBy, String rawPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: id=" + id));

        // Islemi yapan admin (performedBy JWT'den gelir -> güvenilir).
        User actingAdmin = userRepository.findByEmail(performedBy)
                .orElseThrow(() -> new NotFoundException("İşlemi yapan yönetici bulunamadı."));

        // 1) Admin KENDI rolunu degistiremez (kendini kilitlemesini onler).
        if (user.getId().equals(actingAdmin.getId())) {
            throw new SelfRoleChangeException("Kendi rolünüzü değiştiremezsiniz.");
        }

        // 2) Sifre tekrar dogrulamasi (re-authentication). Acik kalmis oturumu ele geciren
        //    biri sifreyi bilmeden rol yukseltemesin diye hassas isleme sifre kapisi koyduk.
        if (!passwordEncoder.matches(rawPassword, actingAdmin.getPassword())) {
            throw new ReauthenticationFailedException("Şifre hatalı. İşlem iptal edildi.");
        }

        Role oldRole = user.getRole();

        /*
         * SON ADMIN KORUMASI.
         * Bu kontrol olmadan sistem kendini kalici olarak kilitleyebiliyordu:
         * tek admin kendi rolunu USER yaparsa sistemde hic ADMIN kalmaz ve
         * /api/admin/** herkese kapanir. AdminSeeder de kurtarmaz, cunku o sadece
         * "bu e-posta var mi" diye bakar, ROLUNE bakmaz -> "zaten mevcut" deyip ciker.
         * Geri donusun tek yolu DB'ye elle SQL atmak olurdu.
         *
         * Kontrolun yeri burasi (servis), controller degil: bu bir IS KURALI.
         * Ayrica @Transactional icinde oldugu icin sayim ile guncelleme ayni
         * transaction'da - araya baska bir islem girip sayimi gecersiz kilamaz.
         */
        if (oldRole == Role.ADMIN && newRole != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new LastAdminException(
                    "Sistemdeki son yöneticinin yetkisi kaldırılamaz. "
                    + "Önce başka bir kullanıcıyı yönetici yapın.");
        }

        user.setRole(newRole);

        // Yetki degisikligi denetimin en kritik olayidir -> mutlaka iz birakir.
        // performedBy'i controller'dan aliyoruz: "kim yapti" bilgisi is mantiginin
        // degil, oturumun bilgisi; servis SecurityContext'e uzanmasin diye disaridan gecirdik.
        auditService.log("ROLE_CHANGE", performedBy,
                "Kullanıcı " + user.getEmail() + " rolü " + oldRole + " -> " + newRole);

        return toUserResponse(user);
    }

    // ---- entity -> DTO donusumleri (tek yonlu esleme) ----

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getTcNo(),
                user.getEmail(),
                user.getRole().name(),   // enum -> String: API sozlesmesi enum'a bagli kalmasin
                user.getCreatedAt());
        // password ve creditScores'a HIC dokunmuyoruz -> ne sizinti ne lazy-load sorunu.
    }

    private AuditLogResponse toAuditResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getPerformedBy(),
                log.getDetails(),
                log.getCreatedAt());
    }
}
