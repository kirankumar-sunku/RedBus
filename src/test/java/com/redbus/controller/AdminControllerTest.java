package com.redbus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redbus.config.JwtService;
import com.redbus.config.SecurityConfig;
import com.redbus.dto.request.BusRequest;
import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.service.serviceInterface.AdminServiceInter;
import com.redbus.service.serviceInterface.BusServiceInter;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AdminServiceInter adminService;
    @MockBean  BusServiceInter busService;
    @MockBean  JwtService jwtService;

    @BeforeEach
    void configureMocks() {
        when(jwtService.isTokenValid("admin-token")).thenReturn(true);
        when(jwtService.extractUserId("admin-token")).thenReturn(1L);
        when(jwtService.extractEmail("admin-token")).thenReturn("admin@redbus.com");
        when(jwtService.extractRole("admin-token")).thenReturn("ADMIN");

        when(jwtService.isTokenValid("user-token")).thenReturn(true);
        when(jwtService.extractUserId("user-token")).thenReturn(5L);
        when(jwtService.extractEmail("user-token")).thenReturn("user@redbus.com");
        when(jwtService.extractRole("user-token")).thenReturn("USER");
    }

    // ── POST /admin ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /admin (admin sign-in)")
    class AdminSignInTests {

        @Test
        @DisplayName("valid credentials → 200 OK with JWT")
        void success_returns200WithJwt() throws Exception {
            SignInRequest req = new SignInRequest();
            req.setEmail("admin@redbus.com");
            req.setPassword("Admin@1234");

            JwtResponse jwt = new JwtResponse("Admin login successful", "admin.jwt.token", 3600000L);
            when(adminService.signIn(any())).thenReturn(jwt);

            mockMvc.perform(post("/admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("admin.jwt.token"))
                    .andExpect(jsonPath("$.message").value("Admin login successful"));
        }

        @Test
        @DisplayName("service throws → 500 Internal Server Error")
        void serviceThrows_returns500() throws Exception {
            SignInRequest req = new SignInRequest();
            req.setEmail("admin@redbus.com");
            req.setPassword("wrong");

            when(adminService.signIn(any())).thenThrow(new RuntimeException("Invalid credentials"));

            mockMvc.perform(post("/admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ── POST /addBus ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /addBus")
    class AddBusTests {

        @Test
        @DisplayName("ADMIN role, valid request → 200 OK")
        void adminRole_returns200() throws Exception {
            BusRequest req = new BusRequest();
            req.setTravelsName("VRL Travels");
            req.setFromLocation("Bengaluru");
            req.setToLocation("Hyderabad");
            req.setDateOfJourney("2026-07-15");
            req.setPrice(new BigDecimal("650.00"));
            req.setTotalSeats(50);
            req.setAvailableSeatsCount(45);

            when(busService.addBus(any())).thenReturn(Map.of("message", "Bus added successfully", "busId", "10"));

            mockMvc.perform(post("/addBus")
                            .header("Authorization", "Bearer admin-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Bus added successfully"))
                    .andExpect(jsonPath("$.busId").value("10"));
        }

        @Test
        @DisplayName("USER role → 403 Forbidden")
        void userRole_returns403() throws Exception {
            BusRequest req = new BusRequest();
            req.setTravelsName("VRL Travels");

            mockMvc.perform(post("/addBus")
                            .header("Authorization", "Bearer user-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("service throws → 500 Internal Server Error")
        void serviceThrows_returns500() throws Exception {
            BusRequest req = new BusRequest();
            req.setTravelsName("VRL Travels");

            when(busService.addBus(any())).thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(post("/addBus")
                            .header("Authorization", "Bearer admin-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ── PUT /updateBus/{busId} ────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /updateBus/{busId}")
    class UpdateBusTests {

        @Test
        @DisplayName("ADMIN role, valid request → 200 OK")
        void adminRole_returns200() throws Exception {
            BusRequest req = new BusRequest();
            req.setTravelsName("Updated VRL Travels");
            req.setPrice(new BigDecimal("700.00"));

            when(busService.updateBus(anyLong(), any())).thenReturn(Map.of("message", "Bus updated successfully"));

            mockMvc.perform(put("/updateBus/1")
                            .header("Authorization", "Bearer admin-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Bus updated successfully"));
        }

        @Test
        @DisplayName("USER role → 403 Forbidden")
        void userRole_returns403() throws Exception {
            BusRequest req = new BusRequest();
            req.setTravelsName("Updated Travels");

            mockMvc.perform(put("/updateBus/1")
                            .header("Authorization", "Bearer user-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("bus not found → service throws → 500")
        void serviceThrows_returns500() throws Exception {
            BusRequest req = new BusRequest();
            req.setTravelsName("Updated Travels");

            when(busService.updateBus(anyLong(), any())).thenThrow(new RuntimeException("Bus not found"));

            mockMvc.perform(put("/updateBus/99")
                            .header("Authorization", "Bearer admin-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ── DELETE /deleteBus/{busId} ─────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /deleteBus/{busId}")
    class DeleteBusTests {

        @Test
        @DisplayName("ADMIN role → 200 OK")
        void adminRole_returns200() throws Exception {
            when(busService.deleteBus(anyLong())).thenReturn(Map.of("message", "Bus deleted successfully"));

            mockMvc.perform(delete("/deleteBus/1")
                            .header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Bus deleted successfully"));
        }

        @Test
        @DisplayName("USER role → 403 Forbidden")
        void userRole_returns403() throws Exception {
            mockMvc.perform(delete("/deleteBus/1")
                            .header("Authorization", "Bearer user-token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("bus not found → service throws → 500")
        void serviceThrows_returns500() throws Exception {
            when(busService.deleteBus(anyLong())).thenThrow(new RuntimeException("Bus not found"));

            mockMvc.perform(delete("/deleteBus/99")
                            .header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ── GET /getAllBuses ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /getAllBuses")
    class GetAllBusesTests {

        @Test
        @DisplayName("ADMIN role → 200 OK with list")
        void adminRole_returns200() throws Exception {
            when(busService.getAllBuses()).thenReturn(List.of());

            mockMvc.perform(get("/getAllBuses")
                            .header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("USER role → 403 Forbidden (method security)")
        void userRole_returns403() throws Exception {
            mockMvc.perform(get("/getAllBuses")
                            .header("Authorization", "Bearer user-token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("service throws → 500 Internal Server Error")
        void serviceThrows_returns500() throws Exception {
            when(busService.getAllBuses()).thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(get("/getAllBuses")
                            .header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ── POST /adminSignOut ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /adminSignOut")
    class AdminSignOutTests {

        @Test
        @DisplayName("ADMIN role, valid Bearer token → 200 OK")
        void adminRole_validToken_returns200() throws Exception {
            when(adminService.signOut(any())).thenReturn(Map.of("message", "Admin signed out successfully"));

            mockMvc.perform(post("/adminSignOut")
                            .header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Admin signed out successfully"));
        }

        @Test
        @DisplayName("non-Bearer Authorization header → 401 Unauthorized")
        void nonBearerHeader_returns401() throws Exception {
            mockMvc.perform(post("/adminSignOut")
                            .header("Authorization", "Basic dXNlcjpwYXNz"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("no authentication → 401 Unauthorized")
        void noAuth_returns401() throws Exception {
            mockMvc.perform(post("/adminSignOut"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
