package com.redbus.service.serviceInterface;

import com.redbus.dto.request.SignInRequest;
import com.redbus.dto.response.JwtResponse;
import com.redbus.exception.RedBusException;

import java.util.Map;

public interface AdminServiceInter {

    JwtResponse signIn(SignInRequest request) throws RedBusException;

    Map<String, String> signOut(String token) throws RedBusException;

}
