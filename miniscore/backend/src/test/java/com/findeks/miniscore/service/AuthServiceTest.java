package com.findeks.miniscore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.findeks.miniscore.dto.AuthResponse;
import com.findeks.miniscore.dto.AuthStepResponse;
import com.findeks.miniscore.dto.LoginRequest;
import com.findeks.miniscore.dto.RegisterRequest;
import com.findeks.miniscore.entity.RefreshToken;
import com.findeks.miniscore.entity.Role;
import com.findeks.miniscore.entity.User;
import com.findeks.miniscore.exception.EmailAlreadyExistsException;
import com.findeks.miniscore.repository.UserRepository;
import com.findeks.miniscore.security.JwtService;

/**
 * AuthService birim testleri.
 *
 * Neden @SpringBootTest DEGIL? Cunku burada Spring context'ine (dolayisiyla
 * Postgres'e) ihtiyac yok: bagimliliklari Mockito ile taklit ediyoruz. Test
 * saniyeler yerine milisaniyede kosar ve DB olmadan CI'da calisir.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authManager;
    @Mock private AuditService auditService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private SettingService settingService;
    @Mock private VerificationService verificationService;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Ali");
        req.setLastName("Veli");
        req.setTcNo("12345678901");
        req.setEmail("ali@test.com");
        req.setPassword("Test1234!");
        return req;
    }

    @Test
    void register_2faKapali_tokenVeRefreshDoner() {
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Test1234!")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(settingService.isEmailTwoFactorEnabled()).thenReturn(false);
        when(jwtService.generateToken(any(User.class))).thenReturn("ACCESS_JWT");
        when(refreshTokenService.create("ali@test.com"))
                .thenReturn(RefreshToken.builder().token("REFRESH_UUID").userEmail("ali@test.com").build());

        AuthStepResponse res = authService.register(registerRequest());

        assertTrue(!res.isVerificationRequired());
        assertEquals("ACCESS_JWT", res.getAuth().getToken());
        assertEquals("REFRESH_UUID", res.getAuth().getRefreshToken());
        assertEquals("USER", res.getAuth().getRole());   // register herkesi USER yapar
        verify(auditService).log(eq("REGISTER"), eq("ali@test.com"), anyString());
    }

    @Test
    void register_2faAcik_kodIster_tokenVermez() {
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Test1234!")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(settingService.isEmailTwoFactorEnabled()).thenReturn(true);

        AuthStepResponse res = authService.register(registerRequest());

        assertTrue(res.isVerificationRequired());
        assertNull(res.getAuth());
        assertEquals("ali@test.com", res.getEmail());
        verify(verificationService).createAndSend("ali@test.com");
        verify(jwtService, never()).generateToken(any());
        verify(refreshTokenService, never()).create(anyString());
    }

    @Test
    void register_epostaZatenVarsa_hataFirlatir_kaydetmez() {
        when(userRepository.existsByEmail("ali@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(registerRequest()));

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).create(anyString());
    }

    @Test
    void login_hataliSifre_basarisizGirisiLoglarVeYenidenFirlatir() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ali@test.com");
        req.setPassword("yanlis");
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));

        verify(auditService).log(eq("LOGIN_FAILED"), eq("ali@test.com"), anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void verify_dogruKod_tokenDoner() {
        User user = User.builder().email("ali@test.com").role(Role.USER).build();
        when(userRepository.findByEmail("ali@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("ACCESS_JWT");
        when(refreshTokenService.create("ali@test.com"))
                .thenReturn(RefreshToken.builder().token("REFRESH_UUID").userEmail("ali@test.com").build());

        AuthResponse res = authService.verify("ali@test.com", "123456");

        assertEquals("ACCESS_JWT", res.getToken());
        assertEquals("REFRESH_UUID", res.getRefreshToken());
        verify(verificationService).verify("ali@test.com", "123456");
    }
}
