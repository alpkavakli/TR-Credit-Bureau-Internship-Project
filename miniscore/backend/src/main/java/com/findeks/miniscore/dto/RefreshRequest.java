package com.findeks.miniscore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * POST /api/auth/refresh isteğinin gövdesi.
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "refreshToken boş olamaz")
    private String refreshToken;
}
