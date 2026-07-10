package com.redbus.controller;

import com.redbus.dto.request.ForgotPasswordRequest;
import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.request.SignUpRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.exception.RedBusException;
import com.redbus.service.serviceInterface.UserServiceInter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserServiceInter userService;

    @PostMapping("/signUp")
    public ResponseEntity<Map<String, String>> signUp(@RequestBody SignUpRequest request) throws RedBusException {
        try {
            Map<String, String> response = userService.signUp(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            throw new RedBusException("UserController: ", "signUp exception occurred", e);
        }
    }

    @PostMapping("/signIn")
    public ResponseEntity<JwtResponse> signIn(@RequestBody SignInRequest request) throws RedBusException {
        try {
            JwtResponse response = userService.signIn(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RedBusException("UserController: ", "signIn exception occurred", e);
        }
    }

    @PostMapping("/signOut")
    public ResponseEntity<Map<String, String>> signOut(
            @RequestHeader("Authorization") String authHeader) throws RedBusException {
        try{
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new RedBusException("UserController: ", "Invalid Authorization header format", null);
            }
            String token = authHeader.substring(7);
            Map<String, String> response = userService.signOut(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RedBusException("UserController: ", "signOut exception occurred", e);
        }
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequest request) throws RedBusException {
        try {
            Map<String, String> response = userService.forgotPassword(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RedBusException("UserController: ", "forgotPassword exception occurred", e);
        }
    }
}
