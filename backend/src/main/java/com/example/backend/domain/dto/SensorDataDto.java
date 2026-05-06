package com.example.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
    @DecimalMin(value = "0.0", message = "moisturePercent must be at least 0")
    @DecimalMax(value = "100.0", message = "moisturePercent must be at most 100")
    @JsonProperty("moisture_percent")
    private Double moisturePercent;

    @NotNull
    @Min(value = 0, message = "moistureRaw must be at least 0")
    @JsonProperty("moisture_raw")
    private Integer moistureRaw;

    @NotNull
    @JsonProperty("is_raining")
    private Boolean isRaining;

    @NotNull
    @Min(value = 0, message = "rainSensorRaw must be at least 0")
    @JsonProperty("rain_sensor_raw")
    private Integer rainSensorRaw;

    @NotBlank(message = "pumpState is required")
    @JsonProperty("pump_state")
    private String pumpState;

    @NotNull
    @Min(value = 0, message = "pumpRemainingTime must be at least 0")
    @JsonProperty("pump_remaining_time")
    private Integer pumpRemainingTime;

    private LocalDateTime timestamp;
}
