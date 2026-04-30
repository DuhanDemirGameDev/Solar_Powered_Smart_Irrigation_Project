package com.example.backend.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notification_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message; 

    @Column(nullable = false, length = 50)
    private String type; // Örn: "MANUAL_COMMAND", "EMAIL_ALERT"

    @Column(nullable = false)
    private LocalDateTime timestamp;
}