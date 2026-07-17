package com.findeks.miniscore.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.findeks.miniscore.dto.AuditLogResponse;
import com.findeks.miniscore.dto.UserResponse;
import com.findeks.miniscore.entity.AuditLog;
import com.findeks.miniscore.entity.Role;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.exception.LastAdminException;
import com.findeks.miniscore.exception.NotFoundException;
import com.findeks.miniscore.repository.AuditLogRepository;
import com.findeks.miniscore.repository.UserRepository;

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

    // readOnly = true: sadece okuyoruz -> Hibernate "dirty checking" (degisiklik takibi)
    // yapmaz, gereksiz is yapmaz. Ayrica niyet bildirimi: bu metot veri DEGISTIRMEZ.
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)   // entity -> DTO (password disarida kalir)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: id=" + id));
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> listAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toAuditResponse)
                .collect(Collectors.toList());
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
    public UserResponse updateRole(Long id, Role newRole, String performedBy) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı: id=" + id));

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
