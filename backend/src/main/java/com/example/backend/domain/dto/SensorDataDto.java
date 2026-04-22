package com.example.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("moisture_percent")
    private Double moisturePercent;

    @NotNull
    @JsonProperty("moisture_raw")
    private Integer moistureRaw;

    @NotNull
    @JsonProperty("is_raining")
    private Boolean isRaining;

    @NotNull
    @JsonProperty("rain_sensor_raw")
    private Integer rainSensorRaw;

    @NotNull
    @JsonProperty("pump_state")
    private String pumpState;

    @NotNull
    @JsonProperty("pump_remaining_time")
    private Integer pumpRemainingTime;

    private LocalDateTime timestamp;
}
