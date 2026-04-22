package com.example.backend.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "irrigation_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrrigationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String pumpStatus;

    @Column(nullable = false)
    private Integer durationInMinutes;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
