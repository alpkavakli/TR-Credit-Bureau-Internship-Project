package com.findeks.miniscore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.findeks.miniscore.dto.UserResponse;
import com.findeks.miniscore.entity.Role;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.exception.LastAdminException;
import com.findeks.miniscore.exception.ReauthenticationFailedException;
import com.findeks.miniscore.exception.SelfRoleChangeException;
import com.findeks.miniscore.repository.AuditLogRepository;
import com.findeks.miniscore.repository.UserRepository;

/**
 * AdminService.updateRole birim testleri.
 * Kurallar: (1) admin kendi rolunu degistiremez, (2) sifre tekrar dogrulanir,
 * (3) sistemdeki son admin dusurulemez.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditService auditService;
    @Mock private CreditScoreService creditScoreService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AdminService adminService;

    private static final String ADMIN_EMAIL = "admin@findeks.com";
    private static final String ADMIN_HASH = "HASHED_PW";

    private User user(Long id, String email, Role role) {
        return User.builder().id(id).email(email).firstName("Ad").lastName("Soyad")
                .role(role).password(ADMIN_HASH).build();
    }

    @Test
    void updateRole_adminKendiRolunuDegistiremez() {
        User self = user(1L, ADMIN_EMAIL, Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(self));

        assertThrows(SelfRoleChangeException.class,
                () -> adminService.updateRole(1L, Role.USER, ADMIN_EMAIL, "admin12345"));

        assertEquals(Role.ADMIN, self.getRole());
        verify(auditService, never()).log(anyString(), anyString(), anyString());
    }

    @Test
    void updateRole_sifreYanlissa_reddedilir() {
        User target = user(2L, "user@test.com", Role.USER);
        User admin = user(1L, ADMIN_EMAIL, Role.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("yanlis", ADMIN_HASH)).thenReturn(false);

        assertThrows(ReauthenticationFailedException.class,
                () -> adminService.updateRole(2L, Role.ADMIN, ADMIN_EMAIL, "yanlis"));

        assertEquals(Role.USER, target.getRole());
        verify(auditService, never()).log(anyString(), anyString(), anyString());
    }

    @Test
    void updateRole_sonAdminiDusurmeyeCalisirsa_hataFirlatir() {
        User targetAdmin = user(3L, "diger-admin@test.com", Role.ADMIN);
        User admin = user(1L, ADMIN_EMAIL, Role.ADMIN);
        when(userRepository.findById(3L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin12345", ADMIN_HASH)).thenReturn(true);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThrows(LastAdminException.class,
                () -> adminService.updateRole(3L, Role.USER, ADMIN_EMAIL, "admin12345"));

        assertEquals(Role.ADMIN, targetAdmin.getRole());
        verify(auditService, never()).log(anyString(), anyString(), anyString());
    }

    @Test
    void updateRole_dogruSifreyle_kullaniciyiAdminYapar_veDenetimeYazar() {
        User target = user(2L, "user@test.com", Role.USER);
        User admin = user(1L, ADMIN_EMAIL, Role.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin12345", ADMIN_HASH)).thenReturn(true);

        UserResponse res = adminService.updateRole(2L, Role.ADMIN, ADMIN_EMAIL, "admin12345");

        assertEquals("ADMIN", res.getRole());
        assertEquals(Role.ADMIN, target.getRole());
        verify(auditService).log(eq("ROLE_CHANGE"), eq(ADMIN_EMAIL), anyString());
    }
}
