package com.redbus.dto.request;
import lombok.Data;
@Data
public class SeatBlockingRequest {
    private Long busId;
    private Integer seatNumber;
}
