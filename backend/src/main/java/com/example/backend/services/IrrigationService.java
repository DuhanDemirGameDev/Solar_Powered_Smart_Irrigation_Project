package com.example.backend.services;

import com.example.backend.domain.dto.IrrigationLogDto;
import com.example.backend.domain.entities.IrrigationLog;
import com.example.backend.repositories.IrrigationLogRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
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

    private final IrrigationLogRepository irrigationLogRepository;
    private final JavaMailSender mailSender;

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

    // BURAYI PUBLIC YAPTIK - Controller artık burayı görebilir
    public void sendEmailNotification(int duration) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("solarpower0606@gmail.com");
            message.setTo("solarpower0606@gmail.com");
            message.setSubject("🌱 Akıllı Sulama Sistemi Bildirimi");
            message.setText("Sistem nemin düştüğünü fark etti! Pompa " + duration + " dakika boyunca çalıştırılacak.");
            mailSender.send(message);
            System.out.println("✅ Mail başarıyla gönderildi!");
        } catch (Exception e) {
            System.err.println("❌ Mail gönderilirken hata oluştu: " + e.getMessage());
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