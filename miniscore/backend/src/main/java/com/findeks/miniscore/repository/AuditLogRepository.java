package com.findeks.miniscore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.AuditLog;



public interface AuditLogRepository extends JpaRepository<AuditLog, Long>{

    /*
     * "Derived query method": govdesini BIZ yazmiyoruz, Spring Data metodun ADINI
     * okuyup SQL'i kendi uretiyor.
     *   findAll + ByOrderBy + CreatedAt + Desc
     *   -> SELECT * FROM audit_logs ORDER BY created_at DESC
     * Denetim kayitlarinda en yeni olay en ustte olsun istiyoruz.
     *
     * DIKKAT: isim alan adiyla (createdAt) birebir uyusmali; "creationDate" yazsaydik
     * uygulama ACILIRKEN hata verirdi (calisma aninda degil) -> hatayi erken gorurduk.
     */
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
