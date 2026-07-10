package com.redbus.service;

import com.redbus.dto.request.CancelTicketRequest;
import com.redbus.dto.request.PassengerRequest;
import com.redbus.dto.request.SeatBlockingRequest;
import com.redbus.dto.response.HistoryResponse;
import com.redbus.dto.response.PassengerResponse;
import com.redbus.dto.response.SeatBlockingResponse;
import com.redbus.entity.Bus;
import com.redbus.entity.Passenger;
import com.redbus.entity.SeatBlocking;
import com.redbus.exception.RedBusException;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.exception.SeatExpiredException;
import com.redbus.exception.SeatUnavailableException;
import com.redbus.repository.BusRepository;
import com.redbus.repository.PassengerRepository;
import com.redbus.repository.SeatBlockingRepository;
import com.redbus.service.serviceImpl.BookingService;
import com.redbus.service.serviceImpl.BusService;
import com.redbus.validation.CentralizedValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService")
class BookingServiceTest {

    @InjectMocks BookingService bookingService;

    @Mock SeatBlockingRepository seatBlockingRepository;
    @Mock PassengerRepository    passengerRepository;
    @Mock BusRepository          busRepository;
    @Mock BusService             busService;          // concrete class — Mockito handles it fine
    @Mock CentralizedValidator   validator;

    // ── blockSeat ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("blockSeat")
    class BlockSeatTests {

        @Test
        @DisplayName("seat in range and available → blocking saved, response with message returned")
        void success() throws Exception {
            SeatBlockingRequest req = new SeatBlockingRequest();
            req.setBusId(1L);
            req.setSeatNumber(12);

            when(busService.getAvailableSeatNumbers(1L)).thenReturn(List.of(5, 12, 18, 25));

            SeatBlockingResponse response = bookingService.blockSeat(req, 5L);

            assertThat(response.getMessage()).contains("Seat 12 blocked successfully");
            verify(seatBlockingRepository).save(any(SeatBlocking.class));
            verify(busRepository).decrementAvailableSeats(1L);
        }

        @Test
        @DisplayName("seat number 0 (below range) → RedBusException wrapping SeatUnavailableException")
        void seatBelowRange_throwsRedBusException() {
            SeatBlockingRequest req = new SeatBlockingRequest();
            req.setBusId(1L);
            req.setSeatNumber(0);

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> bookingService.blockSeat(req, 5L));

            assertThat(ex.getCause()).isInstanceOf(SeatUnavailableException.class);
            assertThat(ex.getCause().getMessage()).contains("between 1 and 50");
        }

        @Test
        @DisplayName("seat number 51 (above range) → RedBusException wrapping SeatUnavailableException")
        void seatAboveRange_throwsRedBusException() {
            SeatBlockingRequest req = new SeatBlockingRequest();
            req.setBusId(1L);
            req.setSeatNumber(51);

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> bookingService.blockSeat(req, 5L));

            assertThat(ex.getCause()).isInstanceOf(SeatUnavailableException.class);
        }

        @Test
        @DisplayName("requested seat not in available list → RedBusException wrapping SeatUnavailableException")
        void seatNotAvailable_throwsRedBusException() throws Exception {
            SeatBlockingRequest req = new SeatBlockingRequest();
            req.setBusId(1L);
            req.setSeatNumber(10);  // 10 is NOT in available list

            when(busService.getAvailableSeatNumbers(1L)).thenReturn(List.of(5, 12, 18));

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> bookingService.blockSeat(req, 5L));

            assertThat(ex.getCause()).isInstanceOf(SeatUnavailableException.class);
            assertThat(ex.getCause().getMessage()).contains("currently unavailable");
        }
    }

    // ── savePassengerDetails ──────────────────────────────────────────────────

    @Nested
    @DisplayName("savePassengerDetails")
    class SavePassengerDetailsTests {

        @Test
        @DisplayName("valid block, belongs to user, not expired → passenger saved, response returned")
        void success() throws Exception {
            PassengerRequest req = buildPassengerRequest(100L);

            SeatBlocking blocking = SeatBlocking.builder()
                    .id(100L).busId(1L).seatNumber(12).userId(5L)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .isConfirmed(false).build();

            Bus bus = Bus.builder().busId(1L).price(new BigDecimal("650.00")).build();

            when(seatBlockingRepository.findById(100L)).thenReturn(Optional.of(blocking));
            when(busRepository.findById(1L)).thenReturn(Optional.of(bus));

            PassengerResponse response = bookingService.savePassengerDetails(req, 5L);

            assertThat(response.getMessage()).isEqualTo("Booking confirmed!");
            assertThat(response.getSeatNumber()).isEqualTo(12);
            assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("650.00"));
            verify(passengerRepository).save(any(Passenger.class));
            assertThat(blocking.getIsConfirmed()).isTrue();   // block confirmed after booking
        }

        @Test
        @DisplayName("blockingId not found → RedBusException wrapping ResourceNotFoundException")
        void blockNotFound_throwsRedBusException() {
            PassengerRequest req = buildPassengerRequest(999L);
            when(seatBlockingRepository.findById(999L)).thenReturn(Optional.empty());

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> bookingService.savePassengerDetails(req, 5L));

            assertThat(ex.getCause()).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("block belongs to a different user → RedBusException wrapping SeatUnavailableException")
        void blockBelongsToDifferentUser_throwsRedBusException() {
            PassengerRequest req = buildPassengerRequest(100L);

            SeatBlocking blocking = SeatBlocking.builder()
                    .id(100L).busId(1L).seatNumber(12)
                    .userId(99L)   // different user
                    .expiresAt(LocalDateTime.now().plusMinutes(5)).build();

            when(seatBlockingRepository.findById(100L)).thenReturn(Optional.of(blocking));

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> bookingService.savePassengerDetails(req, 5L));

            assertThat(ex.getCause()).isInstanceOf(SeatUnavailableException.class);
        }

        @Test
        @DisplayName("seat block has expired → RedBusException wrapping SeatExpiredException")
        void blockExpired_throwsRedBusException() {
            PassengerRequest req = buildPassengerRequest(100L);

            SeatBlocking blocking = SeatBlocking.builder()
                    .id(100L).busId(1L).seatNumber(12).userId(5L)
                    .expiresAt(LocalDateTime.now().minusMinutes(5))  // expired 5 min ago
                    .build();

            when(seatBlockingRepository.findById(100L)).thenReturn(Optional.of(blocking));

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> bookingService.savePassengerDetails(req, 5L));

            assertThat(ex.getCause()).isInstanceOf(SeatExpiredException.class);
        }

        private PassengerRequest buildPassengerRequest(Long blockingId) {
            PassengerRequest req = new PassengerRequest();
            req.setBlockingId(blockingId);
            req.setName("Ramesh Kumar");
            req.setAge(30);
            req.setGender("MALE");
            req.setMobileNumber("9876543210");
            req.setEmailAddress("ramesh@gmail.com");
            return req;
        }
    }

    // ── getHistory ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory")
    class GetHistoryTests {

        @Test
        @DisplayName("user has bookings → mapped to HistoryResponse list")
        void withBookings_returnsMappedList() throws Exception {
            Passenger p = Passenger.builder()
                    .passengerId(50L).busId(1L).seatNumber(12).name("Ramesh Kumar")
                    .age(30).gender(Passenger.Gender.MALE).mobileNumber("9876543210")
                    .emailAddress("ramesh@gmail.com").price(new BigDecimal("650.00"))
                    .bookedAt(LocalDateTime.now()).build();

            when(passengerRepository.findByUserId(5L)).thenReturn(List.of(p));

            List<HistoryResponse> history = bookingService.getHistory(5L);

            assertThat(history).hasSize(1);
            assertThat(history.get(0).getPassengerId()).isEqualTo(50L);
            assertThat(history.get(0).getName()).isEqualTo("Ramesh Kumar");
            assertThat(history.get(0).getSeatNumber()).isEqualTo(12);
            assertThat(history.get(0).getGender()).isEqualTo("MALE");
        }

        @Test
        @DisplayName("user has no bookings → empty list returned")
        void noBookings_returnsEmptyList() throws Exception {
            when(passengerRepository.findByUserId(anyLong())).thenReturn(List.of());

            List<HistoryResponse> history = bookingService.getHistory(5L);

            assertThat(history).isEmpty();
        }
    }

    // ── cancelTicket ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelTicket")
    class CancelTicketTests {

        @Test
        @DisplayName("valid passengerId, belongs to user → deleted, seat released, success message")
        void success() throws Exception {
            CancelTicketRequest req = new CancelTicketRequest();
            req.setPassengerId(50L);

            Passenger passenger = Passenger.builder()
                    .passengerId(50L).blockingId(100L).busId(1L).build();
            SeatBlocking blocking = SeatBlocking.builder()
                    .id(100L).userId(5L).busId(1L).build();

            when(passengerRepository.findById(50L)).thenReturn(Optional.of(passenger));
            when(seatBlockingRepository.findById(100L)).thenReturn(Optional.of(blocking));

            Map<String, String> result = bookingService.cancelTicket(req, 5L);

            assertThat(result).containsEntry("message",
                    "Ticket cancelled successfully. Your seat has been released.");
            verify(passengerRepository).delete(passenger);
            verify(seatBlockingRepository).delete(blocking);
            verify(busRepository).incrementAvailableSeats(1L);
        }

        @Test
        @DisplayName("passenger not found → RedBusException wrapping ResourceNotFoundException")
        void passengerNotFound_throwsRedBusException() {
            CancelTicketRequest req = new CancelTicketRequest();
            req.setPassengerId(999L);

            when(passengerRepository.findById(999L)).thenReturn(Optional.empty());

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> bookingService.cancelTicket(req, 5L));

            assertThat(ex.getCause()).isInstanceOf(ResourceNotFoundException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("Booking not found");
        }

        @Test
        @DisplayName("booking belongs to different user → RedBusException wrapping ResourceNotFoundException")
        void unauthorizedUser_throwsRedBusException() {
            CancelTicketRequest req = new CancelTicketRequest();
            req.setPassengerId(50L);

            Passenger passenger = Passenger.builder()
                    .passengerId(50L).blockingId(100L).busId(1L).build();
            SeatBlocking blocking = SeatBlocking.builder()
                    .id(100L).userId(99L).busId(1L).build();  // owned by user 99, not 5

            when(passengerRepository.findById(50L)).thenReturn(Optional.of(passenger));
            when(seatBlockingRepository.findById(100L)).thenReturn(Optional.of(blocking));

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> bookingService.cancelTicket(req, 5L));

            assertThat(ex.getCause()).isInstanceOf(ResourceNotFoundException.class);
            assertThat(ex.getCause().getMessage()).contains("not authorized");
        }
    }
}
