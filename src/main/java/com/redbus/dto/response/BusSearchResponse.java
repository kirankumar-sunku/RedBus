package com.redbus.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BusSearchResponse {
    private Long busId;
    private String travelsName;
    private List<Integer> availableSeats;
    private String fromLocation;
    private String toLocation;
    private LocalDate dateOfJourney;
    private BigDecimal price;
    private Integer totalSeats;
    private Integer availableSeatsCount;
}
