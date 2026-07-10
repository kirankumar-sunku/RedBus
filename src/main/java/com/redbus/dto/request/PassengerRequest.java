package com.redbus.dto.request;
import lombok.Data;
@Data
public class PassengerRequest {
    private Long blockingId;
    private String name;
    private Integer age;
    private String gender;
    private String mobileNumber;
    private String emailAddress;
}
