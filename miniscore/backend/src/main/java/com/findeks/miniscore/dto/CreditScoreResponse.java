package com.findeks.miniscore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreditScoreResponse {
    private Integer score;
    private String riskCategory;
    private String firstName;
    private String lastName;
}