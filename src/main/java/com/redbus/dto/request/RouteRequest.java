package com.redbus.dto.request;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class RouteRequest {
    private Long busId;
    private String fromLocation;
    private String toLocation;
    private BigDecimal price;
}
