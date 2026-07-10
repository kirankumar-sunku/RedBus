package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;
}
