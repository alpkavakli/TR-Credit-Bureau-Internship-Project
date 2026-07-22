package com.findeks.miniscore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Basit anahtar-değer sistem ayarı. Şimdilik tek kullanıcısı "email_2fa_enabled".
 *
 * Neden DB? Ayar çalışma zamanında admin tarafından değiştirilebilmeli ve yeniden
 * başlatmadan kalıcı olmalı. application.yml sabittir; bu ise "durum"dur.
 */
@Entity
@Table(name = "app_settings")
@Data @NoArgsConstructor @AllArgsConstructor
public class AppSetting {

    // "key"/"value" bazı veritabanlarında ayrılmış kelime -> kolon adlarını açıkça verdik.
    @Id
    @Column(name = "setting_key")
    private String settingKey;

    @Column(name = "setting_value", nullable = false)
    private String settingValue;
}
