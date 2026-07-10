package com.redbus.service;

import com.redbus.config.JwtService;
import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.entity.Admin;
import com.redbus.exception.AuthenticationException;
import com.redbus.exception.RedBusException;
import com.redbus.repository.AdminRepository;
import com.redbus.service.serviceImpl.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService")
class AdminServiceTest {

    @InjectMocks AdminService adminService;

    @Mock AdminRepository  adminRepository;
    @Mock PasswordEncoder  passwordEncoder;
    @Mock JwtService       jwtService;

    // ── signIn ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("signIn")
    class SignInTests {

        @Test
        @DisplayName("valid credentials → JwtResponse with ADMIN token and WELCOME ADMIN message")
        void success() throws Exception {
            SignInRequest req = new SignInRequest();
            req.setEmail("admin@redbus.com");
            req.setPassword("Admin@1234");

            Admin admin = Admin.builder().id(1L).email("admin@redbus.com").password("hashed-admin").build();
            when(adminRepository.findByEmail("admin@redbus.com")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("Admin@1234", "hashed-admin")).thenReturn(true);
            when(jwtService.generateToken(1L, "admin@redbus.com", "ADMIN")).thenReturn("admin-jwt");

            JwtResponse result = adminService.signIn(req);

            assertThat(result.getToken()).isEqualTo("admin-jwt");
            assertThat(result.getMessage()).isEqualTo("WELCOME ADMIN");
            assertThat(result.getExpiresIn()).isEqualTo(3600);
        }

        @Test
        @DisplayName("admin email not in DB → RedBusException wrapping AuthenticationException")
        void adminNotFound_throwsRedBusException() {
            SignInRequest req = new SignInRequest();
            req.setEmail("nobody@redbus.com");
            req.setPassword("Admin@1234");

            when(adminRepository.findByEmail(any())).thenReturn(Optional.empty());

            RedBusException ex = assertThrows(RedBusException.class, () -> adminService.signIn(req));
            assertThat(ex.getCause()).isInstanceOf(AuthenticationException.class);
        }

        @Test
        @DisplayName("wrong password → RedBusException wrapping AuthenticationException")
        void wrongPassword_throwsRedBusException() {
            SignInRequest req = new SignInRequest();
            req.setEmail("admin@redbus.com");
            req.setPassword("WrongPass@1");

            Admin admin = Admin.builder().id(1L).email("admin@redbus.com").password("hashed-admin").build();
            when(adminRepository.findByEmail(any())).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            RedBusException ex = assertThrows(RedBusException.class, () -> adminService.signIn(req));
            assertThat(ex.getCause()).isInstanceOf(AuthenticationException.class);
        }
    }

    // ── signOut ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("signOut")
    class SignOutTests {

        @Test
        @DisplayName("valid token → token blacklisted, success message returned")
        void success() throws Exception {
            Map<String, String> result = adminService.signOut("admin-token");

            assertThat(result).containsEntry("message", "Signed out successfully");
            verify(jwtService).blacklistToken("admin-token");
        }
    }
}
