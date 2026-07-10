package com.redbus.service;

import com.redbus.config.JwtService;
import com.redbus.dto.request.ForgotPasswordRequest;
import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.request.SignUpRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.entity.Passenger;
import com.redbus.entity.SeatBlocking;
import com.redbus.entity.User;
import com.redbus.exception.AuthenticationException;
import com.redbus.exception.DuplicateUserException;
import com.redbus.exception.RedBusException;
import com.redbus.exception.ResourceNotFoundException;
import com.redbus.exception.ValidationException;
import com.redbus.repository.PassengerRepository;
import com.redbus.repository.SeatBlockingRepository;
import com.redbus.repository.UserRepository;
import com.redbus.service.serviceImpl.UserService;
import com.redbus.validation.CentralizedValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @InjectMocks UserService userService;

    @Mock UserRepository            userRepository;
    @Mock SeatBlockingRepository    seatBlockingRepository;
    @Mock PassengerRepository       passengerRepository;
    @Mock CentralizedValidator      validator;
    @Mock PasswordEncoder           passwordEncoder;
    @Mock JwtService                jwtService;
    @Mock StringRedisTemplate       redisTemplate;

    // ── signUp ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("signUp")
    class SignUpTests {

        @Test
        @DisplayName("all validations pass, no duplicate → user saved, success message returned")
        void success() throws Exception {
            when(validator.validateAndParseDob(any())).thenReturn(LocalDate.of(1990, 1, 1));
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByMobileNumber(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashedPwd");

            Map<String, String> result = userService.signUp(buildSignUpRequest());

            assertThat(result).containsEntry("message", "Successfully registered Please Sign in");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("email already registered → RedBusException wrapping DuplicateUserException")
        void duplicateEmail_throwsRedBusException() throws Exception {
            when(validator.validateAndParseDob(any())).thenReturn(LocalDate.of(1990, 1, 1));
            when(userRepository.existsByEmail(any())).thenReturn(true);

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> userService.signUp(buildSignUpRequest()));

            assertThat(ex.getCause()).isInstanceOf(DuplicateUserException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("mobile already registered → RedBusException wrapping DuplicateUserException")
        void duplicateMobile_throwsRedBusException() throws Exception {
            when(validator.validateAndParseDob(any())).thenReturn(LocalDate.of(1990, 1, 1));
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByMobileNumber(any())).thenReturn(true);

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> userService.signUp(buildSignUpRequest()));

            assertThat(ex.getCause()).isInstanceOf(DuplicateUserException.class);
        }

        @Test
        @DisplayName("validator rejects name → RedBusException wrapping ValidationException")
        void invalidName_throwsRedBusException() throws Exception {
            doThrow(new ValidationException("Name invalid")).when(validator).validateName(any());

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> userService.signUp(buildSignUpRequest()));

            assertThat(ex.getCause()).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("validator rejects password → RedBusException wrapping ValidationException")
        void weakPassword_throwsRedBusException() throws Exception {
            when(validator.validateAndParseDob(any())).thenReturn(LocalDate.of(1990, 1, 1));
            doThrow(new ValidationException("Password too weak")).when(validator).validatePassword(any());

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> userService.signUp(buildSignUpRequest()));

            assertThat(ex.getCause()).isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("invalid email → RedBusException wrapping ValidationException")
        void invalidEmail_throwsRedBusException() throws Exception {
            doThrow(new ValidationException("Invalid email")).when(validator).validateEmail(any());

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> userService.signUp(buildSignUpRequest()));

            assertThat(ex.getCause()).isInstanceOf(ValidationException.class);
        }

        private SignUpRequest buildSignUpRequest() {
            SignUpRequest req = new SignUpRequest();
            req.setName("Ramesh Kumar");
            req.setMobileNumber("9876543210");
            req.setDateOfBirth("01011990");
            req.setEmail("ramesh@gmail.com");
            req.setAddress("123 MG Road Bengaluru");
            req.setPassword("Ram@1234");
            return req;
        }
    }

    // ── signIn ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("signIn")
    class SignInTests {

        @Test
        @DisplayName("valid credentials → JwtResponse with token and WELCOME message")
        void success() throws Exception {
            SignInRequest req = new SignInRequest();
            req.setEmail("ramesh@gmail.com");
            req.setPassword("Ram@1234");

            User user = User.builder().id(5L).email("ramesh@gmail.com").password("hashed").build();
            when(userRepository.findByEmail("ramesh@gmail.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Ram@1234", "hashed")).thenReturn(true);
            when(jwtService.generateToken(5L, "ramesh@gmail.com", "USER")).thenReturn("jwt-token");

            JwtResponse result = userService.signIn(req);

            assertThat(result.getToken()).isEqualTo("jwt-token");
            assertThat(result.getMessage()).isEqualTo("WELCOME TO THE REDBUS");
        }

        @Test
        @DisplayName("email not registered → RedBusException wrapping AuthenticationException")
        void userNotFound_throwsRedBusException() throws Exception {
            SignInRequest req = new SignInRequest();
            req.setEmail("nobody@gmail.com");
            req.setPassword("Ram@1234");

            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            RedBusException ex = assertThrows(RedBusException.class, () -> userService.signIn(req));
            assertThat(ex.getCause()).isInstanceOf(AuthenticationException.class);
        }

        @Test
        @DisplayName("wrong password → RedBusException wrapping AuthenticationException")
        void wrongPassword_throwsRedBusException() throws Exception {
            SignInRequest req = new SignInRequest();
            req.setEmail("ramesh@gmail.com");
            req.setPassword("WrongPass@1");

            User user = User.builder().id(5L).email("ramesh@gmail.com").password("hashed").build();
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            RedBusException ex = assertThrows(RedBusException.class, () -> userService.signIn(req));
            assertThat(ex.getCause()).isInstanceOf(AuthenticationException.class);
        }
    }

    // ── signOut ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("signOut")
    class SignOutTests {

        @Test
        @DisplayName("token blacklisted and all 3 cache regions evicted for the user")
        void evictsAllUserCaches() throws Exception {
            SeatBlocking block = SeatBlocking.builder().busId(2L).seatNumber(7).build();
            Passenger passenger = Passenger.builder().blockingId(100L).build();

            when(jwtService.extractUserId("valid-token")).thenReturn(5L);
            when(seatBlockingRepository.findByUserId(5L)).thenReturn(List.of(block));
            when(passengerRepository.findByUserId(5L)).thenReturn(List.of(passenger));

            Map<String, String> result = userService.signOut("valid-token");

            assertThat(result).containsEntry("message", "Signed out successfully");
            verify(jwtService).blacklistToken("valid-token");
            verify(redisTemplate).delete("bookingHistory::5");
            verify(redisTemplate).delete("seatHolds::2:7");
            verify(redisTemplate).delete("passenger::100");
        }

        @Test
        @DisplayName("user with no blocks or passengers → only bookingHistory cache deleted")
        void noBlocksOrPassengers_evictsOnlyHistory() throws Exception {
            when(jwtService.extractUserId("clean-token")).thenReturn(3L);
            when(seatBlockingRepository.findByUserId(3L)).thenReturn(List.of());
            when(passengerRepository.findByUserId(3L)).thenReturn(List.of());

            userService.signOut("clean-token");

            verify(redisTemplate).delete("bookingHistory::3");
            verify(redisTemplate, times(1)).delete(anyString());
        }
    }

    // ── forgotPassword ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("valid request → password re-encoded, saved, success message returned")
        void success() throws Exception {
            User user = User.builder().id(5L).email("ramesh@gmail.com").password("old-hash").build();
            when(userRepository.findByEmail("ramesh@gmail.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewPass@123")).thenReturn("new-hash");
            when(seatBlockingRepository.findByUserId(anyLong())).thenReturn(List.of());
            when(passengerRepository.findByUserId(anyLong())).thenReturn(List.of());

            Map<String, String> result = userService.forgotPassword(buildRequest("NewPass@123", "NewPass@123"));

            assertThat(result.get("message")).contains("Password updated successfully");
            verify(userRepository).save(user);
            assertThat(user.getPassword()).isEqualTo("new-hash");
        }

        @Test
        @DisplayName("email not found → RedBusException wrapping ResourceNotFoundException")
        void emailNotFound_throwsRedBusException() throws Exception {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> userService.forgotPassword(buildRequest("NewPass@123", "NewPass@123")));

            assertThat(ex.getCause()).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("newPassword != confirmPassword → RedBusException wrapping ValidationException")
        void passwordsMismatch_throwsRedBusException() throws Exception {
            User user = User.builder().id(5L).email("ramesh@gmail.com").password("old").build();
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> userService.forgotPassword(buildRequest("NewPass@123", "Different@456")));

            assertThat(ex.getCause()).isInstanceOf(ValidationException.class);
            assertThat(ex.getCause().getMessage()).contains("do not match");
        }

        @Test
        @DisplayName("new password fails validator → RedBusException wrapping ValidationException")
        void weakNewPassword_throwsRedBusException() throws Exception {
            User user = User.builder().id(5L).email("ramesh@gmail.com").password("old").build();
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
            doThrow(new ValidationException("Password too weak")).when(validator).validatePassword("weak");

            RedBusException ex = assertThrows(RedBusException.class,
                    () -> userService.forgotPassword(buildRequest("weak", "weak")));

            assertThat(ex.getCause()).isInstanceOf(ValidationException.class);
        }

        private ForgotPasswordRequest buildRequest(String newPwd, String confirmPwd) {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("ramesh@gmail.com");
            req.setNewPassword(newPwd);
            req.setConfirmPassword(confirmPwd);
            return req;
        }
    }
}
