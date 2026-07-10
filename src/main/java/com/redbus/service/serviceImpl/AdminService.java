package com.redbus.service.serviceImpl;

import com.redbus.config.JwtService;
import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.entity.Admin;
import com.redbus.exception.AuthenticationException;
import com.redbus.exception.RedBusException;
import com.redbus.repository.AdminRepository;
import com.redbus.service.serviceInterface.AdminServiceInter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService implements AdminServiceInter {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public JwtResponse signIn(SignInRequest request) throws RedBusException {
        try {
            Admin admin = adminRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AuthenticationException("Invalid admin credentials"));

            if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
                throw new AuthenticationException("Invalid admin credentials");
            }

            String token = jwtService.generateToken(admin.getId(), admin.getEmail(), "ADMIN");

            return new JwtResponse("WELCOME ADMIN", token, 3600);
        } catch (Exception e) {
            throw new RedBusException("AdminService: ", "signIn exception occurred", e);
        }
    }

    public Map<String, String> signOut(String token) throws RedBusException {
        try {
            jwtService.blacklistToken(token);
            return Map.of("message", "Signed out successfully");
        } catch (Exception e) {
            throw new RedBusException("UserService: ", "signOut exception occurred", e);
        }
    }

}
