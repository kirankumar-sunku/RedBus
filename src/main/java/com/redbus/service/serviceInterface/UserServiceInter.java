package com.redbus.service.serviceInterface;

import com.redbus.dto.request.ForgotPasswordRequest;
import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.request.SignUpRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.exception.RedBusException;

import java.util.Map;

public interface UserServiceInter {

    Map<String, String> signUp(SignUpRequest request) throws RedBusException;

    JwtResponse signIn(SignInRequest request) throws RedBusException;

    Map<String, String> signOut(String token) throws RedBusException;

    Map<String, String> forgotPassword(ForgotPasswordRequest request) throws RedBusException;
}
