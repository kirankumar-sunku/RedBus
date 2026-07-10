package com.redbus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redbus.config.JwtService;
import com.redbus.dto.request.CancelTicketRequest;
import com.redbus.dto.request.PassengerRequest;
import com.redbus.dto.request.SeatBlockingRequest;
import com.redbus.dto.response.HistoryResponse;
import com.redbus.dto.response.PassengerResponse;
import com.redbus.dto.response.SeatBlockingResponse;
import com.redbus.config.SecurityConfig;
import com.redbus.service.serviceInterface.BookingServiceInter;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
@DisplayName("BookingController")
class BookingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  BookingServiceInter bookingService;
    @MockBean  JwtService jwtService;

    @BeforeEach
    void configureMocks() {
        when(jwtService.isTokenValid(any())).thenReturn(true);
        when(jwtService.extractUserId(any())).thenReturn(5L);
        when(jwtService.extractEmail(any())).thenReturn("user@test.com");
        when(jwtService.extractRole(any())).thenReturn("USER");
    }

    // ── POST /seatBlocking ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /seatBlocking")
    class BlockSeatTests {

        @Test
        @DisplayName("valid request → 200 OK with blockingId and expiry")
        void success_returns200() throws Exception {
            SeatBlockingRequest req = new SeatBlockingRequest();
            req.setBusId(1L);
            req.setSeatNumber(12);

            SeatBlockingResponse response = new SeatBlockingResponse(
                    "Seat 12 blocked successfully. You have 10 minutes to complete passenger details.",
                    100L,
                    LocalDateTime.now().plusMinutes(10)
            );
            when(bookingService.blockSeat(any(), anyLong())).thenReturn(response);

            mockMvc.perform(post("/seatBlocking")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blockingId").value(100));
        }

        @Test
        @DisplayName("service throws (seat unavailable) → 500")
        void serviceThrows_returns500() throws Exception {
            SeatBlockingRequest req = new SeatBlockingRequest();
            req.setBusId(1L);
            req.setSeatNumber(5);

            when(bookingService.blockSeat(any(), anyLong()))
                    .thenThrow(new RuntimeException("Seat 5 is currently unavailable"));

            mockMvc.perform(post("/seatBlocking")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("unauthenticated request → 401 Unauthorized")
        void noAuth_returns401() throws Exception {
            SeatBlockingRequest req = new SeatBlockingRequest();
            req.setBusId(1L);
            req.setSeatNumber(12);

            mockMvc.perform(post("/seatBlocking")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── POST /passengerDetails ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /passengerDetails")
    class PassengerDetailsTests {

        @Test
        @DisplayName("valid request → 201 Created with passenger details")
        void success_returns201() throws Exception {
            PassengerRequest req = new PassengerRequest();
            req.setBlockingId(100L);
            req.setName("Ramesh Kumar");
            req.setAge(30);
            req.setGender("MALE");
            req.setMobileNumber("9876543210");
            req.setEmailAddress("ramesh@gmail.com");

            PassengerResponse response = PassengerResponse.builder()
                    .message("Booking confirmed!")
                    .passengerId(50L)
                    .seatNumber(12)
                    .busId(1L)
                    .price(new BigDecimal("650.00"))
                    .build();

            when(bookingService.savePassengerDetails(any(), anyLong())).thenReturn(response);

            mockMvc.perform(post("/passengerDetails")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.passengerId").value(50))
                    .andExpect(jsonPath("$.message").value("Booking confirmed!"))
                    .andExpect(jsonPath("$.seatNumber").value(12));
        }

        @Test
        @DisplayName("seat block expired → service throws → 500")
        void serviceThrows_returns500() throws Exception {
            PassengerRequest req = new PassengerRequest();
            req.setBlockingId(999L);

            when(bookingService.savePassengerDetails(any(), anyLong()))
                    .thenThrow(new RuntimeException("Seat block not found"));

            mockMvc.perform(post("/passengerDetails")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("unauthenticated request → 401 Unauthorized")
        void noAuth_returns401() throws Exception {
            PassengerRequest req = new PassengerRequest();
            req.setBlockingId(100L);

            mockMvc.perform(post("/passengerDetails")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /history ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /history")
    class HistoryTests {

        @Test
        @DisplayName("user has bookings → 200 OK with booking list")
        void withBookings_returns200() throws Exception {
            HistoryResponse booking = HistoryResponse.builder()
                    .passengerId(50L)
                    .name("Ramesh Kumar")
                    .age(30)
                    .gender("MALE")
                    .mobileNumber("9876543210")
                    .emailAddress("ramesh@gmail.com")
                    .price(new BigDecimal("650.00"))
                    .busId(1L)
                    .seatNumber(12)
                    .build();

            when(bookingService.getHistory(anyLong())).thenReturn(List.of(booking));

            mockMvc.perform(get("/history")
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].passengerId").value(50))
                    .andExpect(jsonPath("$[0].name").value("Ramesh Kumar"))
                    .andExpect(jsonPath("$[0].seatNumber").value(12));
        }

        @Test
        @DisplayName("user has no bookings → 200 OK with empty list")
        void noBookings_returnsEmptyList() throws Exception {
            when(bookingService.getHistory(anyLong())).thenReturn(List.of());

            mockMvc.perform(get("/history")
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("service throws → 500 Internal Server Error")
        void serviceThrows_returns500() throws Exception {
            when(bookingService.getHistory(anyLong()))
                    .thenThrow(new RuntimeException("DB connection failed"));

            mockMvc.perform(get("/history")
                            .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("unauthenticated request → 401 Unauthorized")
        void noAuth_returns401() throws Exception {
            mockMvc.perform(get("/history"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── DELETE /cancelTicket ──────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /cancelTicket")
    class CancelTicketTests {

        @Test
        @DisplayName("valid passengerId → 200 OK with success message")
        void success_returns200() throws Exception {
            CancelTicketRequest req = new CancelTicketRequest();
            req.setPassengerId(50L);

            when(bookingService.cancelTicket(any(), anyLong()))
                    .thenReturn(Map.of("message", "Ticket cancelled successfully. Your seat has been released."));

            mockMvc.perform(delete("/cancelTicket")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value("Ticket cancelled successfully. Your seat has been released."));
        }

        @Test
        @DisplayName("booking not found → service throws → 500")
        void serviceThrows_returns500() throws Exception {
            CancelTicketRequest req = new CancelTicketRequest();
            req.setPassengerId(999L);

            when(bookingService.cancelTicket(any(), anyLong()))
                    .thenThrow(new RuntimeException("Booking not found"));

            mockMvc.perform(delete("/cancelTicket")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("unauthenticated request → 401 Unauthorized")
        void noAuth_returns401() throws Exception {
            CancelTicketRequest req = new CancelTicketRequest();
            req.setPassengerId(50L);

            mockMvc.perform(delete("/cancelTicket")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
