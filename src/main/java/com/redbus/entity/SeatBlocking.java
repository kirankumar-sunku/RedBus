package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat_blocking",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_active_seat",
           columnNames = {"bus_id", "seat_number"}
       ))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class SeatBlocking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bus_id", nullable = false)
    private Long busId;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed = false;
}
