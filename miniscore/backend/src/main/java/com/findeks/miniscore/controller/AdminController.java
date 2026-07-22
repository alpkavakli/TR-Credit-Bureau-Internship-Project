package com.findeks.miniscore.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.findeks.miniscore.dto.AuditLogResponse;
import com.findeks.miniscore.dto.CreditScoreResponse;
import com.findeks.miniscore.dto.SettingsResponse;
import com.findeks.miniscore.dto.UpdateEmail2faRequest;
import com.findeks.miniscore.dto.UpdateRoleRequest;
import com.findeks.miniscore.dto.UserResponse;
import com.findeks.miniscore.service.AdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Sadece ADMIN rolunun erisebildigi yonetim uc noktalari.
 *
 * BURADA IKI KATLI KORUMA VAR, ikisi de bilinerek duruyor:
 *
 * 1) URL SEVIYESI  -> SecurityConfig'teki .requestMatchers("/api/admin/**").hasRole("ADMIN")
 *    Filtre zincirinde, controller'a GIRMEDEN once calisir. Kaba ama saglam:
 *    yeni bir admin metodu eklendiginde onu korumayi UNUTSAN bile kural gecerlidir.
 *
 * 2) METOT SEVIYESI -> asagidaki @PreAuthorize
 *    Spring AOP proxy'si ile metot cagrilmadan hemen once calisir. Ince ayar yapabilir:
 *    "hasRole('ADMIN') or #id == authentication.principal.id" gibi kurallar yazilabilir.
 *
 * Ikisi birden = "defense in depth". Biri kaldirilirsa/yanlis yazilirsa digeri tutar.
 *
 * ⚠️ TUZAK: @PreAuthorize'in calismasi icin @EnableMethodSecurity SART.
 * Yoksa Spring bu annotation'i SESSIZCE gormezden gelir -> hata vermez, sadece korumaz.
 * (SecurityConfig'e ekledik.)
 *
 * Sinif seviyesine koyduk -> icerideki TUM metotlara uygulanir.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

   // Controller repository'ye DEGIL servise bagli -> katman kurali.
   private final AdminService adminService;

   /**
    * hasRole('ADMIN') aslinda "ROLE_ADMIN" yetkisini arar; "ROLE_" onekini Spring
    * kendisi ekler. User.getAuthorities() de "ROLE_" + role.name() donduruyor
    * -> ikisi zaten uyumlu. (hasAuthority('ADMIN') yazsaydik ESLESMEZDI.)
    */
   @GetMapping("/users")
   public ResponseEntity<List<UserResponse>> listUsers() {
       return ResponseEntity.ok(adminService.listUsers());
   }

   // @PathVariable: URL'deki {id} parcasini metot parametresine baglar.
   @GetMapping("/users/{id}")
   public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
       // Kullanici yoksa AdminService NotFoundException firlatir
       // -> GlobalExceptionHandler onu 404'e cevirir. Burada try/catch YOK.
       return ResponseEntity.ok(adminService.getUser(id));
   }

   /**
    * Bir kullanicinin findeks skor gecmisi (guncel skor = listenin son elemani).
    * currentUser = bakan admin; denetim kaydinda "kim gordu" olarak yazilir.
    */
   @GetMapping("/users/{id}/scores")
   public ResponseEntity<List<CreditScoreResponse>> getUserScores(
           @PathVariable Long id,
           @AuthenticationPrincipal UserDetails currentUser) {
       return ResponseEntity.ok(adminService.getUserScores(id, currentUser.getUsername()));
   }

   /**
    * Denetim kayitlari sayfali doner. @PageableDefault: istemci parametre gondermezse
    * en yeni 20 kayit (createdAt DESC). Istemci ?page=1&size=50 ile gezinebilir.
    * Yanit gövdesi: { content: [...], totalPages, totalElements, number, size, ... }.
    */
   // ---- Sistem ayarları: e-posta 2FA açık/kapalı ----

   @GetMapping("/settings")
   public ResponseEntity<SettingsResponse> getSettings() {
       return ResponseEntity.ok(adminService.getSettings());
   }

   @PutMapping("/settings/email-2fa")
   public ResponseEntity<SettingsResponse> setEmailTwoFactor(
           @Valid @RequestBody UpdateEmail2faRequest request,
           @AuthenticationPrincipal UserDetails currentUser) {
       return ResponseEntity.ok(
               adminService.setEmailTwoFactor(request.getEnabled(), currentUser.getUsername()));
   }

   @GetMapping("/audit-logs")
   public ResponseEntity<Page<AuditLogResponse>> listAuditLogs(
           @RequestParam(required = false) String search,
           @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
           Pageable pageable) {
       return ResponseEntity.ok(adminService.listAuditLogs(search, pageable));
   }

   /**
    * Kullanicinin rolunu degistirir.
    * PUT kullaniyoruz cunku islem "idempotent": ayni istegi 10 kez gondersen de
    * sonuc ayni (rol o deger olur). POST "her cagri yeni bir sey yaratir" demektir.
    *
    * @AuthenticationPrincipal: JWT'den cozulen oturum sahibini verir. Rolu KIMIN
    * degistirdigini audit'e yazabilmek icin lazim.
    */
   @PutMapping("/users/{id}/role")
   public ResponseEntity<UserResponse> updateRole(
           @PathVariable Long id,
           @Valid @RequestBody UpdateRoleRequest request,
           @AuthenticationPrincipal UserDetails currentUser) {
       return ResponseEntity.ok(
               adminService.updateRole(id, request.getRole(),
                       currentUser.getUsername(), request.getPassword()));
   }
}
