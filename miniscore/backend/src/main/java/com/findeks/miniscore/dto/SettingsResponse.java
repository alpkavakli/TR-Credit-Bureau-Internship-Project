package com.findeks.miniscore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Admin panelinin okuduğu sistem ayarları. */
@Data
@AllArgsConstructor
public class SettingsResponse {
    private boolean emailTwoFactorEnabled;
}
