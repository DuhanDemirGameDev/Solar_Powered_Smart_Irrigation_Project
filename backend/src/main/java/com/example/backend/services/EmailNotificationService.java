package com.example.backend.services;

import com.example.backend.domain.entities.NotificationLog;
import com.example.backend.repositories.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailNotificationService {
    
    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;

    // Duhan'ın sensör verisi gelirken çağıracağı Acil Durum fonksiyonu
    public void sendEmergencyAlert(String to, String moistureLevel) {
        String msgContent = "ACİL: Toprak nemi kritik seviyede: %" + moistureLevel;
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Akıllı Sulama Sistemi Uyarısı");
            message.setText(msgContent);
            mailSender.send(message);

            saveLog(msgContent,"EMAIL_ALERT");
        } catch (Exception e) {
            System.err.println("Mail gönderim hatası: " + e.getMessage());
        }
    }

    // Sisteme log kaydeden yardımcı fonksiyon
    public void saveLog(String message, String type) {
        NotificationLog log = NotificationLog.builder()
                .message(message)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build();
        notificationLogRepository.save(log);
    }
}