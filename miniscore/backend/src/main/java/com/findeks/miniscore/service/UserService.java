package com.findeks.miniscore.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.findeks.miniscore.dto.UserResponse;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.exception.NotFoundException;
import com.findeks.miniscore.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Kullanicinin KENDI profili ile ilgili is mantigi (admin islemleri AdminService'te).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return toResponse(findOrThrow(email));
    }

    /**
     * Ad/soyad guncelleme. save() cagirmiyoruz: entity transaction icinde "managed",
     * Hibernate dirty-checking ile UPDATE'i kendi atar (AdminService.updateRole ile ayni desen).
     */
    @Transactional
    public UserResponse updateProfile(String email, String firstName, String lastName) {
        User user = findOrThrow(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        auditService.log("PROFILE_UPDATE", email,
                "Ad/soyad güncellendi: " + firstName + " " + lastName);

        return toResponse(user);
    }

    private User findOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Kullanıcı bulunamadı."));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getTcNo(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt());
    }
}
