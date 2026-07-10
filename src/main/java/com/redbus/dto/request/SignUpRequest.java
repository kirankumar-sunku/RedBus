package com.redbus.dto.request;

import lombok.Data;

@Data
public class SignUpRequest {
    private String name;
    private String mobileNumber;
    private String dateOfBirth;  // DDMMYYYY format
    private String email;
    private String address;
    private String password;
}
