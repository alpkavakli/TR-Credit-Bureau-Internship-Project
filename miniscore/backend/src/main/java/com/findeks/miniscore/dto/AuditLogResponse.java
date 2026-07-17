package com.findeks.miniscore.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Denetim kaydinin API gorunumu.
 *
 * AuditLog entity'sinde su an hassas bir alan yok, yani "entity'yi dondursek de
 * olurdu" denebilir. Yine de DTO yaziyoruz cunku:
 *  - Yarin AuditLog'a "ipAddress" veya "sessionId" eklenirse, DTO olmadan o alan
 *    otomatik olarak API'den disari sizardi. DTO ile ne eklenirse eklensin
 *    disari SADECE burada yazan alanlar cikar.
 *  - Kural tutarli olsun: hicbir controller entity dondurmez. Istisnasi olmayan
 *    kurali uygulamak, "bu entity zararsiz mi" diye her seferinde dusunmekten iyidir.
 */
@Data
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private String action;         // SCORE_QUERY | LOGIN | LOGIN_FAILED | REGISTER | ROLE_CHANGE
    private String performedBy;    // islemi yapanin e-postasi
    private String details;
    private LocalDateTime createdAt;
}
