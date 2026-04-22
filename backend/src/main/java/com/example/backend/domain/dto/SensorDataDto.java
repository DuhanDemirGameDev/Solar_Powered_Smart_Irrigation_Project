package com.example.backend.domain.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDataDto {

    private Long id;

    @NotNull
    private Double soilMoistureLevel;

    @NotNull
    private Boolean isRaining;

    private LocalDateTime timestamp;
}
