package com.redbus.service.serviceImpl;

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
import com.redbus.service.serviceInterface.BookingServiceInter;
import com.redbus.validation.CentralizedValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService implements BookingServiceInter {

    private final SeatBlockingRepository seatBlockingRepository;
    private final PassengerRepository passengerRepository;
    private final BusRepository busRepository;
    private final BusService busService;
    private final CentralizedValidator validator;

    // ********************** Seat Blocking ***************************************************
    // @Cacheable(value = "availableSeats", key = "#request.busId()") // June 16
    //  @CachePut(value = "seatHolds", key = "#request.busId() + ':' + #request.seatNumber()") // June 16
    @Transactional
   // @CacheEvict(value = "availableSeats", key = "#request.busId()")
    @CachePut(value = "seatHolds", key = "#p0.busId + ':' + #p0.seatNumber", unless = "#result == null") // June 17
    public SeatBlockingResponse blockSeat(SeatBlockingRequest request, Long userId) throws RedBusException {
        System.out.println(" BookingService1 blockSeat = ");
        System.out.println("blockSeat service1 = " + LocalTime.now());
        try {
            Long busId = request.getBusId();
            Integer seatNumber = request.getSeatNumber();

            // Validate seat range
            if (seatNumber < 1 || seatNumber > 50) {
                throw new SeatUnavailableException("Seat number must be between 1 and 50.");
            }

            // Check if seat is currently available
            List<Integer> available = busService.getAvailableSeatNumbers(busId);
            System.out.println("blockSeat available = " + available);
            if (!available.contains(seatNumber)) {
                throw new SeatUnavailableException(
                        "Seat " + seatNumber + " is currently unavailable. Please choose another seat.");
            }

            LocalDateTime now = LocalDateTime.now();
            SeatBlocking blocking = SeatBlocking.builder()
                    .busId(busId)
                    .seatNumber(seatNumber)
                    .userId(userId)
                    .blockedAt(now)
                    .expiresAt(now.plusMinutes(10))
                    .isConfirmed(false)
                    .build();

            seatBlockingRepository.save(blocking);
            busRepository.decrementAvailableSeats(busId);

            return new SeatBlockingResponse(
                    "Seat " + seatNumber + " blocked successfully. You have 10 minutes to complete passenger details.",
                    blocking.getId(),
                    blocking.getExpiresAt()
            );
        } catch (Exception e) {
            throw new RedBusException("BookingService: ", "blockSeat exception occurred", e);
        }
    }

    // ******************** Passenger Details ***************************************
    @Caching(
        put  = @CachePut(value = "passenger", key = "#p0.blockingId", unless = "#result == null || #p0.blockingId == null"),
        evict = @CacheEvict(value = "bookingHistory", key = "#userId")
    )
    public PassengerResponse savePassengerDetails(PassengerRequest request, Long userId) throws RedBusException {
        try {
            System.out.println("savePassengerDetails service1 = " + LocalTime.now());
        // Fetch the seat blocking record
        System.out.println("blocking.getBusId() = " + request.getBlockingId());
        SeatBlocking blocking = seatBlockingRepository.findById(request.getBlockingId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat block not found"));
        System.out.println("blocking.getBusId() = " + blocking.getBusId());
        // Verify the block belongs to this user
        if (!blocking.getUserId().equals(userId)) {
            throw new SeatUnavailableException("This seat block does not belong to you.");
        }

        // Check if block has expired
        if (LocalDateTime.now().isAfter(blocking.getExpiresAt())) {
            throw new SeatExpiredException(
                    "Your seat block has expired. Please restart the booking process.");
        }

        // Validate passenger details using CentralizedValidator (same rules as SignUp)
        validator.validateName(request.getName());
        validator.validateAge(request.getAge());
        validator.validateMobileNumber(request.getMobileNumber());
        validator.validateEmail(request.getEmailAddress());

        // Get bus to copy price
        Bus bus = busRepository.findById(blocking.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found"));

        Passenger passenger = Passenger.builder()
                .blockingId(blocking.getId())
                .busId(blocking.getBusId())
                .seatNumber(blocking.getSeatNumber())
                .name(request.getName())
                .age(request.getAge())
                .gender(Passenger.Gender.valueOf(request.getGender().toUpperCase()))
                .mobileNumber(request.getMobileNumber())
                .emailAddress(request.getEmailAddress())
                .price(bus.getPrice())
                .bookedAt(LocalDateTime.now())
                .build();

        passengerRepository.save(passenger);

        // Confirm the seat block
        blocking.setIsConfirmed(true);
        seatBlockingRepository.save(blocking);

        return PassengerResponse.builder()
                .message("Booking confirmed!")
                .passengerId(passenger.getPassengerId())
                .seatNumber(passenger.getSeatNumber())
                .busId(passenger.getBusId())
                .price(passenger.getPrice())
                .build();
    } catch (Exception e) {
            throw new RedBusException("BookingService: ", "savePassengerDetails exception occurred", e);
        }
    }

    // ── History ───────────────────────────────────────────────────────────────
    @Cacheable(value = "bookingHistory", key = "#userId")    // June 16
    public List<HistoryResponse> getHistory(Long userId) throws RedBusException {
        try {
            System.out.println("getHistory service1 = " + LocalTime.now());
            return passengerRepository.findByUserId(userId).stream()
                    .map(p -> HistoryResponse.builder()
                            .passengerId(p.getPassengerId())
                            .name(p.getName())
                            .age(p.getAge())
                            .gender(p.getGender().name())
                            .mobileNumber(p.getMobileNumber())
                            .emailAddress(p.getEmailAddress())
                            .price(p.getPrice())
                            .busId(p.getBusId())
                            .seatNumber(p.getSeatNumber())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RedBusException("BookingService: ", "getHistory exception occurred", e);
        }
    }

    // ── Cancel Ticket ─────────────────────────────────────────────────────────


    @Transactional
    @CacheEvict(value = "bookingHistory", key = "#userId")
    public Map<String, String> cancelTicket(CancelTicketRequest request, Long userId) throws RedBusException {
        try {
            System.out.println("cancelTicket in service= ");
            System.out.println("cancelTicket service1 = " + LocalTime.now());
            Passenger passenger = passengerRepository.findById(request.getPassengerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

            // Verify this booking belongs to requesting user
            SeatBlocking blocking = seatBlockingRepository.findById(passenger.getBlockingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Seat block not found"));
            System.out.println("cancelTicket in service2= ");
            if (!blocking.getUserId().equals(userId)) {
                throw new ResourceNotFoundException("You are not authorized to cancel this booking.");
            }
            System.out.println("cancelTicket in service3= ");
            // ATOMIC: delete passenger → delete blocking → release seat
            passengerRepository.delete(passenger);
            seatBlockingRepository.delete(blocking);
            busRepository.incrementAvailableSeats(passenger.getBusId());

            return Map.of("message", "Ticket cancelled successfully. Your seat has been released.");
        } catch (Exception e) {
            throw new RedBusException("BookingService: ", "cancelTicket exception occurred", e);
        }
    }
}
