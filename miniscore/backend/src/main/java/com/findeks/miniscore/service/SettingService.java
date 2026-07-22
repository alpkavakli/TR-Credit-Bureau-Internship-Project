package com.findeks.miniscore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.findeks.miniscore.entity.AppSetting;
import com.findeks.miniscore.repository.AppSettingRepository;

import lombok.RequiredArgsConstructor;

/**
 * Sistem ayarlarını okuyup yazan servis. Şimdilik tek ayar: e-posta 2FA açık/kapalı.
 * Varsayılan KAPALI -> e-posta yapılandırılmadan da uygulama sorunsuz çalışır; admin
 * hazır olunca panelden açar.
 */
@Service
@RequiredArgsConstructor
public class SettingService {

    public static final String EMAIL_2FA = "email_2fa_enabled";

    private final AppSettingRepository appSettingRepository;

    @Transactional(readOnly = true)
    public boolean isEmailTwoFactorEnabled() {
        return appSettingRepository.findById(EMAIL_2FA)
                .map(s -> Boolean.parseBoolean(s.getSettingValue()))
                .orElse(false);   // kayıt yoksa varsayılan: kapalı
    }

    @Transactional
    public void setEmailTwoFactorEnabled(boolean enabled) {
        appSettingRepository.save(new AppSetting(EMAIL_2FA, Boolean.toString(enabled)));
    }
}
