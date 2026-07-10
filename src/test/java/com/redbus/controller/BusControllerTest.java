package com.redbus.controller;

import com.redbus.config.JwtService;
import com.redbus.config.SecurityConfig;
import com.redbus.dto.response.BusSearchResponse;
import com.redbus.service.serviceInterface.BusServiceInter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusController.class)
@Import(SecurityConfig.class)
@DisplayName("BusController")
class BusControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  BusServiceInter busService;
    @MockBean  JwtService jwtService;   // needed by JwtAuthFilter in context

    // /BusSearch is permitAll() — no @WithMockUser needed

    // ── GET /BusSearch ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /BusSearch")
    class SearchBusesTests {

        @Test
        @DisplayName("valid params → 200 OK with matching buses")
        void validParams_returns200WithBuses() throws Exception {
            BusSearchResponse bus = BusSearchResponse.builder()
                    .busId(1L)
                    .travelsName("VRL Travels")
                    .fromLocation("Bengaluru")
                    .toLocation("Hyderabad")
                    .dateOfJourney(LocalDate.of(2026, 7, 15))
                    .price(new BigDecimal("650.00"))
                    .availableSeatsCount(40)
                    .totalSeats(50)
                    .availableSeats(List.of(3, 7, 12, 18, 25))
                    .build();

            when(busService.searchBuses("Bengaluru", "Hyderabad", "2026-07-15"))
                    .thenReturn(List.of(bus));

            mockMvc.perform(get("/BusSearch")
                            .param("from", "Bengaluru")
                            .param("to", "Hyderabad")
                            .param("dateOfJourney", "2026-07-15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].busId").value(1))
                    .andExpect(jsonPath("$[0].travelsName").value("VRL Travels"))
                    .andExpect(jsonPath("$[0].availableSeatsCount").value(40));
        }

        @Test
        @DisplayName("no buses on route → 200 OK with empty list")
        void noResults_returnsEmptyList() throws Exception {
            when(busService.searchBuses(any(), any(), any())).thenReturn(List.of());

            mockMvc.perform(get("/BusSearch")
                            .param("from", "Chennai")
                            .param("to", "Pune")
                            .param("dateOfJourney", "2026-07-20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("missing required param (dateOfJourney) → 500")
        void missingParam_returns500() throws Exception {
            // MissingServletRequestParameterException is caught by GlobalExceptionHandler.handleGeneral → 500
            mockMvc.perform(get("/BusSearch")
                            .param("from", "Bengaluru")
                            .param("to", "Hyderabad"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("service throws → 500 Internal Server Error")
        void serviceThrows_returns500() throws Exception {
            when(busService.searchBuses(any(), any(), any()))
                    .thenThrow(new RuntimeException("DB connection failed"));

            mockMvc.perform(get("/BusSearch")
                            .param("from", "Bengaluru")
                            .param("to", "Hyderabad")
                            .param("dateOfJourney", "2026-07-15"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
