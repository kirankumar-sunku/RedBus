package com.redbus.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data @AllArgsConstructor
public class JwtResponse {
    private String message;
    private String token;
    private long expiresIn;
}
