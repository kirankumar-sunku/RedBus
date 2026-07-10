package com.redbus.service.serviceImpl;

import com.redbus.config.JwtService;
import com.redbus.dto.request.ForgotPasswordRequest;
import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.request.SignUpRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.entity.User;
import com.redbus.exception.AuthenticationException;
import com.redbus.exception.DuplicateUserException;
import com.redbus.exception.RedBusException;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.exception.ValidationException;
import com.redbus.repository.PassengerRepository;
import com.redbus.repository.SeatBlockingRepository;
import com.redbus.repository.UserRepository;
import com.redbus.service.serviceInterface.UserServiceInter;
import com.redbus.validation.CentralizedValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceInter {

    private final UserRepository userRepository;
    private final SeatBlockingRepository seatBlockingRepository;
    private final PassengerRepository passengerRepository;
    private final CentralizedValidator validator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    public Map<String, String> signUp(SignUpRequest request) throws RedBusException {
        try {
            // Validate all fields via CentralizedValidator
            validator.validateName(request.getName());
            validator.validateMobileNumber(request.getMobileNumber());
            LocalDate dob = validator.validateAndParseDob(request.getDateOfBirth());
            validator.validateEmail(request.getEmail());
            validator.validateAddress(request.getAddress());
            validator.validatePassword(request.getPassword());

            // Check for duplicates
            if (userRepository.existsByEmail(request.getEmail()) ||
                    userRepository.existsByMobileNumber(request.getMobileNumber())) {
                throw new DuplicateUserException(
                        "The details provided are already available in the system. " +
                                "Please sign in using your existing account.");
            }

            User user = User.builder()
                    .name(request.getName())
                    .mobileNumber(request.getMobileNumber())
                    .dateOfBirth(dob)
                    .email(request.getEmail())
                    .address(request.getAddress())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .build();

            userRepository.save(user);

            return Map.of("message", "Successfully registered Please Sign in");
        } catch (Exception e) {
            throw new RedBusException("UserService: ", "signUp exception occurred", e);
        }
    }

    public JwtResponse signIn(SignInRequest request) throws RedBusException {
        try {
            // Validate email and password format
            validator.validateEmail(request.getEmail());
            validator.validatePassword(request.getPassword());

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new AuthenticationException("Invalid email or password");
            }

            String token = jwtService.generateToken(user.getId(), user.getEmail(), "USER");

            return new JwtResponse("WELCOME TO THE REDBUS", token, 3600);
        } catch (Exception e) {
            throw new RedBusException("UserService: ", "signUp exception occurred", e);
        }
    }


    public Map<String, String> signOut(String token) throws RedBusException {
        try {
            Long userId = jwtService.extractUserId(token);
            jwtService.blacklistToken(token);
            evictUserCache(userId);
            return Map.of("message", "Signed out successfully");
        } catch (Exception e) {
            throw new RedBusException("UserService: ", "signOut exception occurred", e);
        }
    }

    public Map<String, String> forgotPassword(ForgotPasswordRequest request) throws RedBusException {
        try {
            validator.validateEmail(request.getEmail());

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("No account found with the provided email address"));

            validator.validatePassword(request.getNewPassword());

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new ValidationException("New password and confirm password do not match");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            evictUserCache(user.getId());

            return Map.of("message", "Password updated successfully. Please sign in with your new password.");
        } catch (Exception e) {
            throw new RedBusException("UserService: ", "forgotPassword exception occurred", e);
        }
    }

    private void evictUserCache(Long userId) {
        // 1. Booking history — keyed directly by userId
        redisTemplate.delete("bookingHistory::" + userId);

        // 2. Seat holds — keyed by busId:seatNumber, look up user's blocks from DB
        seatBlockingRepository.findByUserId(userId).forEach(block ->
            redisTemplate.delete("seatHolds::" + block.getBusId() + ":" + block.getSeatNumber())
        );

        // 3. Passenger details — keyed by blockingId, look up user's passengers from DB
        passengerRepository.findByUserId(userId).forEach(passenger ->
            redisTemplate.delete("passenger::" + passenger.getBlockingId())
        );
    }
}
