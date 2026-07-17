package com.findeks.miniscore.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.findeks.miniscore.entity.Role;
import com.findeks.miniscore.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    public boolean existsByEmail(String email);

    public Optional<User> findByEmail(String email); //Optional olmalı güvenlik için

    /*
     * Derived query: "countBy" + alan adi -> SELECT COUNT(*) FROM users WHERE role = ?
     * AdminService.updateRole kullaniyor: sistemdeki SON admin'in yetkisi
     * elinden alinip sistemin adminsiz kalmasini engellemek icin.
     * COUNT sorgusu DB'de calisir; tum kullanicilari cekip Java'da saymaktan ucuzdur.
     */
    public long countByRole(Role role);

}
