package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "passenger")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long passengerId;

    @Column(name = "blocking_id", nullable = false, unique = true)
    private Long blockingId;

    @Column(name = "bus_id", nullable = false)
    private Long busId;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(name = "mobile_number", nullable = false, length = 10)
    private String mobileNumber;

    @Column(name = "email_address", nullable = false, length = 150)
    private String emailAddress;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
