package com.findeks.miniscore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.findeks.miniscore.entity.AuditLog;
import com.findeks.miniscore.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * Denetim (audit) kayitlarini yazan TEK yer.
 *
 * Neden ayri bir servis?
 * Once bu kod CreditScoreService'in icinde AuditLog.builder()... seklinde duruyordu.
 * LOGIN ve REGISTER icin de ayni sey AuthService'e kopyalanacakti -> ayni mantik 3 yerde.
 * Tek yere topladik: yarin audit'e "IP adresi" alani eklemek istersek tek dosya degisir.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * REQUIRES_NEW = "beni cagiran transaction ne olursa olsun, BEN kendi transaction'imi acarim".
     *
     * Neden onemli: queryScore() @Transactional. Diyelim skor kaydedildi, audit yazildi,
     * sonra metodun sonunda bir hata patladi -> tum transaction geri alinir (rollback) ve
     * audit kaydi da SILINIR. Ama denetim kaydinin amaci tam da "ne oldu"yu tutmaktir;
     * basarisiz denemeler bile iz birakmalidir. REQUIRES_NEW ile audit kendi basina
     * commit olur ve disaridaki rollback onu etkilemez.
     *
     * DIKKAT (Spring tuzagi): bu ancak BASKA bir bean'den cagrilirsa calisir.
     * Spring proxy uzerinden calistigi icin ayni sinif icinden this.log(...) cagrilsaydi
     * proxy devreye girmez, annotation SESSIZCE gormezden gelinirdi.
     * Burada AuthService/CreditScoreService disaridan cagiriyor -> sorun yok.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String performedBy, String details) {
        auditLogRepository.save(AuditLog.builder()
                .action(action)             // SCORE_QUERY | LOGIN | REGISTER | ROLE_CHANGE
                .performedBy(performedBy)   // islemi yapanin e-postasi
                .details(details)           // serbest metin ek bilgi
                .build());
        // createdAt'i BIZ set etmiyoruz -> AuditLog'daki @CreationTimestamp'i
        // Hibernate INSERT aninda otomatik dolduruyor.
    }
}
