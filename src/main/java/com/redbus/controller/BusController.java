package com.redbus.controller;

import com.redbus.dto.response.BusSearchResponse;
import com.redbus.exception.RedBusException;
import com.redbus.service.serviceInterface.BusServiceInter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BusController {

    private final BusServiceInter busService;

    @GetMapping("/BusSearch")
    public ResponseEntity<List<BusSearchResponse>> searchBuses(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String dateOfJourney) throws RedBusException {
        try {
            System.out.println("BusController");
            return ResponseEntity.ok(busService.searchBuses(from, to, dateOfJourney));
        } catch (Exception e) {
            throw new RedBusException("BusController: ","searchBuses exception occurred",e);
        }
    }
}
