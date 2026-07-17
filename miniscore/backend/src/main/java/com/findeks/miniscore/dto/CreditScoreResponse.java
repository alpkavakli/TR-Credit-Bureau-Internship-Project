package com.findeks.miniscore.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

// DTO = Data Transfer Object. Entity'yi dogrudan disari acmak yerine,
// istemciye SADECE gostermek istedigimiz alanlari tasiyan sade bir tasima nesnesi.
@Data               // Lombok: getter/setter/toString... (Jackson bunlari JSON'a cevirir)
@AllArgsConstructor // Lombok: tum alanlari alan constructor -> new CreditScoreResponse(...)
public class CreditScoreResponse {
    private Integer score;
    private String riskCategory;
    private String firstName;
    private String lastName;
    private LocalDate weekStart;   // o snapshot'in haftasinin baslangic tarihi (weekIndex'ten turetilir)
}
