package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "bus")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long busId;

    @Column(name = "travels_name", nullable = false, length = 100)
    private String travelsName;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats_count", nullable = false)
    private Integer availableSeatsCount;

    @Column(name = "from_location", nullable = false, length = 100)
    private String fromLocation;

    @Column(name = "to_location", nullable = false, length = 100)
    private String toLocation;

    @Column(name = "date_of_journey", nullable = false)
    private LocalDate dateOfJourney;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
