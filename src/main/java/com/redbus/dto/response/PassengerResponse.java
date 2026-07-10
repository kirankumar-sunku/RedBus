package com.redbus.dto.response;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data @Builder
public class PassengerResponse {
    private String message;
    private Long passengerId;
    private Integer seatNumber;
    private Long busId;
    private BigDecimal price;
}
