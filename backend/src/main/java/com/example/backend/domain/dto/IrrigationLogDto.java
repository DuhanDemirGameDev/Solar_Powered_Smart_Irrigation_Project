package com.example.backend.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrrigationLogDto {

    private Long id;

    @NotBlank
    @Pattern(regexp = "ON|OFF", message = "pumpStatus must be ON or OFF")
    private String pumpStatus;

    @NotNull
    @Min(0)
    private Integer durationInMinutes;

    private LocalDateTime timestamp;
}
