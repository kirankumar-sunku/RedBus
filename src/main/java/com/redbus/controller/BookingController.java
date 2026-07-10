package com.redbus.controller;

import com.redbus.config.JwtService;
import com.redbus.dto.request.CancelTicketRequest;
import com.redbus.dto.request.PassengerRequest;
import com.redbus.dto.request.SeatBlockingRequest;
import com.redbus.dto.response.HistoryResponse;
import com.redbus.dto.response.PassengerResponse;
import com.redbus.dto.response.SeatBlockingResponse;
import com.redbus.exception.RedBusException;
import com.redbus.service.serviceInterface.BookingServiceInter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingServiceInter bookingService;
    private final JwtService jwtService;

    @PostMapping("/seatBlocking")
    public ResponseEntity<SeatBlockingResponse> blockSeat(
            @RequestBody SeatBlockingRequest request,
            @RequestHeader("Authorization") String authHeader) throws RedBusException {
        try {
            Long userId = extractUserId(authHeader);
            System.out.println("BookingController = ");
            SeatBlockingResponse response = bookingService.blockSeat(request, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RedBusException("BookingController: ", "blockSeat exception occurred", e);
        }

    }

    @PostMapping("/passengerDetails")
    public ResponseEntity<PassengerResponse> savePassengerDetails(
            @RequestBody PassengerRequest request,
            @RequestHeader("Authorization") String authHeader) throws RedBusException {
        try {
            System.out.println("passengerDetails Controller = ");
            Long userId = extractUserId(authHeader);
            PassengerResponse response = bookingService.savePassengerDetails(request, userId);
            System.out.println("response.getPassengerId() = " + response.getPassengerId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            throw new RedBusException("BookingController: ", "passengerDetails exception occurred", e);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<HistoryResponse>> getHistory(
            @RequestHeader("Authorization") String authHeader) throws RedBusException {
        try {
            Long userId = extractUserId(authHeader);
            return ResponseEntity.ok(bookingService.getHistory(userId));
        } catch (Exception e) {
            throw new RedBusException("BookingController: ", "getHistory exception occurred", e);
        }
    }

    @DeleteMapping("/cancelTicket")
    public ResponseEntity<Map<String, String>> cancelTicket(
            @RequestBody CancelTicketRequest request,
            @RequestHeader("Authorization") String authHeader) throws RedBusException {
        try {
            Long userId = extractUserId(authHeader);
            System.out.println("BookingController cancelTicket = " + userId);
            return ResponseEntity.ok(bookingService.cancelTicket(request, userId));
        } catch (Exception e) {
            throw new RedBusException("BookingController: ", "cancelTicket exception occurred", e);
        }
    }

    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RedBusException("BookingController: ", "Invalid Authorization header format", null);
        }
        return jwtService.extractUserId(authHeader.substring(7));
    }
}
