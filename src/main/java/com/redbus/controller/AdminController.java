package com.redbus.controller;

import com.redbus.dto.request.BusRequest;
import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.entity.Bus;
import com.redbus.exception.RedBusException;
import com.redbus.service.serviceInterface.AdminServiceInter;
import com.redbus.service.serviceInterface.BusServiceInter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminServiceInter adminService;
    private final BusServiceInter busService;

    // ── Admin Sign-In ──────────────────────────────────────────────────────────

    @PostMapping("/admin")
    public ResponseEntity<JwtResponse> adminSignIn(@RequestBody SignInRequest request) throws RedBusException {
       try {
           return ResponseEntity.ok(adminService.signIn(request));
       } catch (Exception e) {
           throw new RedBusException("AdminController: ","adminSignIn exception occurred",e);
       }
    }

    // ── Bus Management ─────────────────────────────────────────────────────────

    @PostMapping("/addBus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> addBus(@RequestBody BusRequest request) throws RedBusException {
        try{
            System.out.println("Request Started");
            return ResponseEntity.ok(busService.addBus(request)); // June-07
        } catch (Exception e) {
            throw new RedBusException("AdminController: ","addBus exception occurred",e);
        }
    }

    @PutMapping("/updateBus/{busId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateBus(@PathVariable Long busId, @RequestBody BusRequest request) throws RedBusException {
        try{
            System.out.println("Update Bus Controller");
            return ResponseEntity.ok(busService.updateBus(busId, request));
        } catch (Exception e) {
            throw new RedBusException("AdminController: ","updateBus exception occurred",e);
        }
    }

    @DeleteMapping("/deleteBus/{busId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteBus(@PathVariable Long busId) throws RedBusException {
        try{
            return ResponseEntity.ok(busService.deleteBus(busId));
        } catch (Exception e) {
            throw new RedBusException("AdminController: ","deleteBus exception occurred",e);
        }
    }

    @GetMapping("/getAllBuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Bus>> ListOfBusses() throws RedBusException {
        try{
            return ResponseEntity.ok(busService.getAllBuses());
        } catch (Exception e) {
            throw new RedBusException("AdminController: ","getAllBuses exception occurred",e);
        }
    }

    @PostMapping("/adminSignOut")
    public ResponseEntity<Map<String, String>> signOut(
            @RequestHeader("Authorization") String authHeader) throws RedBusException {
        try{
            System.out.println("AdminController = " + authHeader);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RedBusException("AdminController: ", "Invalid Authorization header format", null);
            }
            String token = authHeader.substring(7);
            Map<String, String> response = adminService.signOut(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RedBusException("AdminController: ", "signOut exception occurred", e);
        }
    }

}
