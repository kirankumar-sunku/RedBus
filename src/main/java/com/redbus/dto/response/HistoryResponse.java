package com.redbus.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HistoryResponse {
    private Long passengerId;
    private String name;
    private Integer age;
    private String gender;
    private String mobileNumber;
    private String emailAddress;
    private BigDecimal price;
    private Long busId;
    private Integer seatNumber;
}
