package com.findeks.miniscore.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** PUT /api/admin/settings/email-2fa gövdesi. */
@Data
public class UpdateEmail2faRequest {

    @NotNull(message = "enabled boş olamaz")
    private Boolean enabled;
}
