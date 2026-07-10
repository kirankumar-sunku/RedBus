package com.redbus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redbus.config.JwtService;
import com.redbus.dto.request.ForgotPasswordRequest;
import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.request.SignUpRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.config.SecurityConfig;
import com.redbus.service.serviceInterface.UserServiceInter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  UserServiceInter userService;
    @MockBean  JwtService jwtService;

    @BeforeEach
    void configureMocks() {
        when(jwtService.isTokenValid(any())).thenReturn(true);
        when(jwtService.extractUserId(any())).thenReturn(5L);
        when(jwtService.extractEmail(any())).thenReturn("user@test.com");
        when(jwtService.extractRole(any())).thenReturn("USER");
    }

    // ── POST /signUp ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /signUp")
    class SignUpTests {

        @Test
        @DisplayName("valid request → 201 Created with success message")
        void success_returns201() throws Exception {
            SignUpRequest req = new SignUpRequest();
            req.setName("Ramesh Kumar");
            req.setEmail("ramesh@gmail.com");
            req.setPassword("Ram@1234");
            req.setMobileNumber("9876543210");
            req.setDateOfBirth("01011990");
            req.setAddress("123 MG Road Bengaluru");

            when(userService.signUp(any())).thenReturn(Map.of("message", "User registered successfully"));

            mockMvc.perform(post("/signUp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("User registered successfully"));
        }

        @Test
        @DisplayName("service throws → 500 Internal Server Error")
        void serviceThrows_returns500() throws Exception {
            SignUpRequest req = new SignUpRequest();
            req.setEmail("ramesh@gmail.com");

            when(userService.signUp(any())).thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(post("/signUp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ── POST /signIn ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /signIn")
    class SignInTests {

        @Test
        @DisplayName("valid credentials → 200 OK with JWT token")
        void success_returns200WithJwt() throws Exception {
            SignInRequest req = new SignInRequest();
            req.setEmail("ramesh@gmail.com");
            req.setPassword("Ram@1234");

            JwtResponse jwt = new JwtResponse("Login successful", "header.payload.signature", 3600000L);
            when(userService.signIn(any())).thenReturn(jwt);

            mockMvc.perform(post("/signIn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("header.payload.signature"))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.expiresIn").value(3600000));
        }

        @Test
        @DisplayName("service throws → 500 Internal Server Error")
        void serviceThrows_returns500() throws Exception {
            SignInRequest req = new SignInRequest();
            req.setEmail("ramesh@gmail.com");
            req.setPassword("WrongPass");

            when(userService.signIn(any())).thenThrow(new RuntimeException("Invalid credentials"));

            mockMvc.perform(post("/signIn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ── POST /signOut ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /signOut")
    class SignOutTests {

        @Test
        @DisplayName("valid Bearer token → 200 OK")
        void validToken_returns200() throws Exception {
            when(userService.signOut(any())).thenReturn(Map.of("message", "Signed out successfully"));

            mockMvc.perform(post("/signOut")
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Signed out successfully"));
        }

        @Test
        @DisplayName("non-Bearer Authorization header → 401 Unauthorized")
        void nonBearerHeader_returns401() throws Exception {
            mockMvc.perform(post("/signOut")
                            .header("Authorization", "Basic dXNlcjpwYXNz"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("no authentication → 401 Unauthorized")
        void noAuth_returns401() throws Exception {
            mockMvc.perform(post("/signOut"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── POST /forgotPassword ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("valid request → 200 OK with success message")
        void success_returns200() throws Exception {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("ramesh@gmail.com");
            req.setNewPassword("NewPass@123");
            req.setConfirmPassword("NewPass@123");

            when(userService.forgotPassword(any())).thenReturn(Map.of("message", "Password updated successfully"));

            mockMvc.perform(post("/forgotPassword")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password updated successfully"));
        }

        @Test
        @DisplayName("email not found → service throws → 500")
        void emailNotFound_returns500() throws Exception {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("notfound@gmail.com");
            req.setNewPassword("NewPass@123");
            req.setConfirmPassword("NewPass@123");

            when(userService.forgotPassword(any())).thenThrow(new RuntimeException("User not found"));

            mockMvc.perform(post("/forgotPassword")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }
    }
}
