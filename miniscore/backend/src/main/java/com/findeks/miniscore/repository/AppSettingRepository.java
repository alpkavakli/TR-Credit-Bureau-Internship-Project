package com.findeks.miniscore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.AppSetting;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
