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
@Table(name = "sensor_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "moisture_percent", nullable = false)
    private Double moisturePercent;

    @Column(name = "moisture_raw", nullable = false)
    private Integer moistureRaw;

    @Column(name = "is_raining", nullable = false)
    private Boolean isRaining;

    @Column(name = "rain_sensor_raw", nullable = false)
    private Integer rainSensorRaw;

    @Column(name = "pump_state", nullable = false, length = 32)
    private String pumpState;

    @Column(name = "pump_remaining_time", nullable = false)
    private Integer pumpRemainingTime;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
