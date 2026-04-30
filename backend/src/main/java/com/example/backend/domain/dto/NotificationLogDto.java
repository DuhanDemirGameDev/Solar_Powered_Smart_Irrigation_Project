package com.example.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogDto {
    private Long id;
    
    @NotBlank
    private String message;
    
    @NotBlank
    private String type;
    
    private LocalDateTime timestamp;
}