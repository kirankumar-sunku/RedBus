package com.redbus.dto.request;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BusRequest {
    private String travelsName;
    private String fromLocation;
    private String toLocation;
    private String dateOfJourney; // yyyy-MM-dd
    private BigDecimal price;
    private Integer totalSeats;
    private Integer availableSeatsCount;
}
