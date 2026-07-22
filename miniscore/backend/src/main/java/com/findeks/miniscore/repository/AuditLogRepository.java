package com.findeks.miniscore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Pageable parametresi -> Spring sorguya LIMIT/OFFSET ekler ve toplam sayimi da
     * yapip Page<T> doner. Denetim tablosu suresiz buyur; hepsini tek seferde cekmek
     * yerine sayfa sayfa istiyoruz. Isimdeki ORDER BY varsayilan siralama; istemci
     * ?sort=... gonderirse Pageable'daki siralama onun yerine gecer.
     *
     * DIKKAT: isim alan adiyla (createdAt) birebir uyusmali; "creationDate" yazsaydik
     * uygulama ACILIRKEN hata verirdi (calisma aninda degil) -> hatayi erken gorurduk.
     */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /*
     * Arama: "yapan" (performedBy = e-posta) alaninda, buyuk/kucuk harf duyarsiz,
     * kismi eslesme. Denetim kayitlarinda kullanici ADI yoktur; kimlik alani yalnizca
     * performedBy e-postasidir, o yuzden arama bunun uzerinden yapilir.
     * Sayfalama korunur -> arama TUM tabloda calisir, sadece acik sayfada degil.
     */
    Page<AuditLog> findByPerformedByContainingIgnoreCaseOrderByCreatedAtDesc(
            String performedBy, Pageable pageable);
}
