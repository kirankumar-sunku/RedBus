package com.redbus.dto.request;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String email;
    private String newPassword;
    private String confirmPassword;
}
