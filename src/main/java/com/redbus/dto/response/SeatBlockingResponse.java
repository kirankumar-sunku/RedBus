package com.redbus.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
@Data @AllArgsConstructor
public class SeatBlockingResponse {
    private String message;
    private Long blockingId;
    private LocalDateTime expiresAt;
}
