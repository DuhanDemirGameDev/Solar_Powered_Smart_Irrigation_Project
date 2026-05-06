package com.example.backend.domain.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PumpCommandDto {
    @NotBlank(message = "Action boş olamaz")
    @Pattern(regexp = "start|stop|none|heat_burst", message = "Geçersiz komut")
    private String action;   
    
    @NotNull
    @Min(value = 0, message = "Süre 0'dan küçük olamaz")
    private Integer duration; 
    
    private String reason;
}