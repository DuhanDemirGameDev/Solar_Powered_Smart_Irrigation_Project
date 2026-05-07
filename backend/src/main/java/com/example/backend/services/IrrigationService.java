package com.example.backend.services;

import com.example.backend.domain.dto.IrrigationLogDto;
import com.example.backend.domain.entities.IrrigationLog;
import com.example.backend.repositories.IrrigationLogRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IrrigationService {

    private static final String ALERT_EMAIL = "solarpower0606@gmail.com";

    private final IrrigationLogRepository irrigationLogRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Transactional
    public IrrigationLogDto logPumpAction(IrrigationLogDto irrigationLogDto) {
        IrrigationLog irrigationLog = IrrigationLog.builder()
                .pumpStatus(irrigationLogDto.getPumpStatus().trim().toUpperCase(Locale.ROOT))
                .durationInMinutes(irrigationLogDto.getDurationInMinutes())
                .timestamp(resolveTimestamp(irrigationLogDto.getTimestamp()))
                .build();

        if ("START".equalsIgnoreCase(irrigationLogDto.getPumpStatus())) {
            sendEmailNotification(irrigationLogDto.getDurationInMinutes());
        }

        IrrigationLog savedLog = irrigationLogRepository.save(irrigationLog);
        return mapToDto(savedLog);
    }

    public void sendEmailNotification(int duration) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(ALERT_EMAIL);
            message.setTo(ALERT_EMAIL);
            message.setSubject("Smart Irrigation System Notification");
            message.setText("The pump will run for " + duration + " minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Mail error: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<IrrigationLogDto> getIrrigationHistory(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return irrigationLogRepository.findAll(pageRequest).map(this::mapToDto);
    }

    private IrrigationLogDto mapToDto(IrrigationLog irrigationLog) {
        return IrrigationLogDto.builder()
                .id(irrigationLog.getId())
                .pumpStatus(irrigationLog.getPumpStatus())
                .durationInMinutes(irrigationLog.getDurationInMinutes())
                .timestamp(irrigationLog.getTimestamp())
                .build();
    }

    private LocalDateTime resolveTimestamp(LocalDateTime timestamp) {
        return timestamp != null ? timestamp : LocalDateTime.now();
    }
}
