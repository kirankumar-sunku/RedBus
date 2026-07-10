package com.redbus.service;

import com.redbus.dto.request.BusRequest;
import com.redbus.dto.response.BusSearchResponse;
import com.redbus.entity.Bus;
import com.redbus.exception.RedBusException;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.repository.BusRepository;
import com.redbus.repository.PassengerRepository;
import com.redbus.service.serviceImpl.BusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusService")
class BusServiceTest {

    @InjectMocks BusService busService;

    @Mock BusRepository       busRepository;
    @Mock PassengerRepository passengerRepository;

    // ── addBus ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addBus")
    class AddBusTests {

        @Test
        @DisplayName("valid request → bus saved, returns map with message and busId")
        void success() throws Exception {
            Bus saved = Bus.builder().busId(10L).travelsName("VRL Travels").build();
            when(busRepository.save(any(Bus.class))).thenReturn(saved);

            Map<String, String> result = busService.addBus(buildBusRequest("2026-07-15"));

            assertThat(result).containsEntry("message", "Bus added successfully");
            assertThat(result).containsEntry("busId", "10");
            verify(busRepository).save(any(Bus.class));
        }

        @Test
        @DisplayName("unparseable date string → RedBusException wrapping IllegalArgumentException")
        void invalidDateFormat_throwsRedBusException() {
            RedBusException ex = assertThrows(RedBusException.class,
                    () -> busService.addBus(buildBusRequest("not-a-date")));

            assertThat(ex.getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(ex.getCause().getMessage()).contains("Invalid date format");
        }

        private BusRequest buildBusRequest(String date) {
            BusRequest req = new BusRequest();
            req.setTravelsName("VRL Travels");
            req.setFromLocation("Bengaluru");
            req.setToLocation("Hyderabad");
            req.setDateOfJourney(date);
            req.setPrice(new BigDecimal("650.00"));
            req.setTotalSeats(50);
            req.setAvailableSeatsCount(45);
            return req;
        }
    }

    // ── updateBus ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateBus")
    class UpdateBusTests {

        @Test
        @DisplayName("bus exists → only provided fields updated, success message returned")
        void success() throws Exception {
            Bus bus = Bus.builder().busId(1L)
                    .travelsName("Old Name").price(new BigDecimal("500.00"))
                    .fromLocation("Bengaluru").toLocation("Hyderabad")
                    .dateOfJourney(LocalDate.of(2026, 7, 10))
                    .totalSeats(50).availableSeatsCount(40).build();

            when(busRepository.findById(1L)).thenReturn(Optional.of(bus));

            BusRequest req = new BusRequest();
            req.setTravelsName("New Name");
            req.setPrice(new BigDecimal("700.00"));

            Map<String, String> result = busService.updateBus(1L, req);

            assertThat(result).containsEntry("message", "Bus updated successfully");
            assertThat(bus.getTravelsName()).isEqualTo("New Name");
            assertThat(bus.getPrice()).isEqualByComparingTo(new BigDecimal("700.00"));
            verify(busRepository).save(bus);
        }

        @Test
        @DisplayName("bus not found → RedBusException wrapping ResourceNotFoundException")
        void busNotFound_throwsRedBusException() {
            when(busRepository.findById(anyLong())).thenReturn(Optional.empty());

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> busService.updateBus(99L, new BusRequest()));

            assertThat(ex.getCause()).isInstanceOf(ResourceNotFoundException.class);
            assertThat(ex.getCause().getMessage()).contains("Bus not found with id: 99");
        }
    }

    // ── deleteBus ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteBus")
    class DeleteBusTests {

        @Test
        @DisplayName("bus exists → deleted from repository, success message returned")
        void success() throws Exception {
            Bus bus = Bus.builder().busId(1L).travelsName("VRL Travels").build();
            when(busRepository.findById(1L)).thenReturn(Optional.of(bus));

            Map<String, String> result = busService.deleteBus(1L);

            assertThat(result).containsEntry("message", "Bus deleted successfully");
            verify(busRepository).delete(bus);
        }

        @Test
        @DisplayName("bus not found → RedBusException wrapping ResourceNotFoundException")
        void busNotFound_throwsRedBusException() {
            when(busRepository.findById(anyLong())).thenReturn(Optional.empty());

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> busService.deleteBus(99L));

            assertThat(ex.getCause()).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getAllBuses ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllBuses")
    class GetAllBusesTests {

        @Test
        @DisplayName("returns all buses from repository")
        void returnsBusList() throws Exception {
            Bus b1 = Bus.builder().busId(1L).travelsName("VRL Travels").build();
            Bus b2 = Bus.builder().busId(2L).travelsName("Orange Travels").build();
            when(busRepository.findAll()).thenReturn(List.of(b1, b2));

            List<Bus> result = busService.getAllBuses();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Bus::getTravelsName)
                    .containsExactly("VRL Travels", "Orange Travels");
        }

        @Test
        @DisplayName("no buses in DB → empty list returned")
        void noBuses_returnsEmptyList() throws Exception {
            when(busRepository.findAll()).thenReturn(List.of());

            assertThat(busService.getAllBuses()).isEmpty();
        }
    }

    // ── searchBuses ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchBuses")
    class SearchBusesTests {

        @Test
        @DisplayName("matching buses found → BusSearchResponse list with available seats populated")
        void success() throws Exception {
            Bus bus = Bus.builder()
                    .busId(1L).travelsName("VRL Travels")
                    .fromLocation("Bengaluru").toLocation("Hyderabad")
                    .dateOfJourney(LocalDate.of(2026, 7, 15))
                    .price(new BigDecimal("650.00"))
                    .totalSeats(50).availableSeatsCount(50).build();

            when(busRepository.findByFromLocationIgnoreCaseAndToLocationIgnoreCaseAndDateOfJourney(
                    any(), any(), any())).thenReturn(List.of(bus));
            when(busRepository.findById(1L)).thenReturn(Optional.of(bus)); // called by getAvailableSeatNumbers
            when(passengerRepository.findBookedSeatNumbersByBusId(1L)).thenReturn(List.of());

            List<BusSearchResponse> results = busService.searchBuses("Bengaluru", "Hyderabad", "2026-07-15");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTravelsName()).isEqualTo("VRL Travels");
            assertThat(results.get(0).getAvailableSeatsCount()).isEqualTo(50);
        }

        @Test
        @DisplayName("no buses on this route/date → empty list returned")
        void noResults_returnsEmptyList() throws Exception {
            when(busRepository.findByFromLocationIgnoreCaseAndToLocationIgnoreCaseAndDateOfJourney(
                    any(), any(), any())).thenReturn(List.of());

            List<BusSearchResponse> results = busService.searchBuses("Chennai", "Pune", "2026-07-20");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("booked seats excluded from available seat list in response")
        void bookedSeatsExcludedFromResponse() throws Exception {
            Bus bus = Bus.builder().busId(1L).travelsName("VRL Travels")
                    .fromLocation("Bengaluru").toLocation("Hyderabad")
                    .dateOfJourney(LocalDate.of(2026, 7, 15))
                    .price(new BigDecimal("650.00"))
                    .totalSeats(50).availableSeatsCount(50).build();

            when(busRepository.findByFromLocationIgnoreCaseAndToLocationIgnoreCaseAndDateOfJourney(
                    any(), any(), any())).thenReturn(List.of(bus));
            when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
            when(passengerRepository.findBookedSeatNumbersByBusId(1L)).thenReturn(List.of(5, 10, 15));

            List<BusSearchResponse> results = busService.searchBuses("Bengaluru", "Hyderabad", "2026-07-15");

            assertThat(results.get(0).getAvailableSeats()).doesNotContain(5, 10, 15);
            assertThat(results.get(0).getAvailableSeatsCount()).isEqualTo(47);
        }
    }

    // ── getAvailableSeatNumbers ────────────────────────────────────────────────

    @Nested
    @DisplayName("getAvailableSeatNumbers")
    class GetAvailableSeatNumbersTests {

        @Test
        @DisplayName("returns generated seat list with booked seats removed")
        void returnsSeatsMinusBooked() throws Exception {
            Bus bus = Bus.builder().busId(1L).availableSeatsCount(50).build();
            when(busRepository.findById(1L)).thenReturn(Optional.of(bus));
            when(passengerRepository.findBookedSeatNumbersByBusId(1L)).thenReturn(List.of(5, 10, 15));

            List<Integer> seats = busService.getAvailableSeatNumbers(1L);

            assertThat(seats).doesNotContain(5, 10, 15);
            assertThat(seats).hasSize(47);          // 50 generated − 3 booked
        }

        @Test
        @DisplayName("all seats available (none booked) → full generated seat list returned")
        void noBookedSeats_returnsAll() throws Exception {
            Bus bus = Bus.builder().busId(2L).availableSeatsCount(50).build();
            when(busRepository.findById(2L)).thenReturn(Optional.of(bus));
            when(passengerRepository.findBookedSeatNumbersByBusId(2L)).thenReturn(List.of());

            List<Integer> seats = busService.getAvailableSeatNumbers(2L);

            assertThat(seats).hasSize(50);
        }

        @Test
        @DisplayName("bus not found → RedBusException wrapping ResourceNotFoundException")
        void busNotFound_throwsRedBusException() {
            when(busRepository.findById(anyLong())).thenReturn(Optional.empty());

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> busService.getAvailableSeatNumbers(99L));

            assertThat(ex.getCause()).isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
