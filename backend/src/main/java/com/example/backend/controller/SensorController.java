package com.example.backend.controller;

import com.example.backend.domain.dto.SensorDataDto;
import com.example.backend.services.SensorService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequiredArgsConstructor
public class SensorController {

    private static final String ALERT_EMAIL = "solarpower0606@gmail.com";

    private final SensorService sensorService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping({"/api/v1/sensors", "/api/sensor-data"})
    public ResponseEntity<SensorDataDto> createSensorData(@Valid @RequestBody SensorDataDto sensorDataDto) {
        SensorDataDto savedSensorData = sensorService.saveSensorData(sensorDataDto);

        updateAiDecision(sensorDataDto);
        sendPumpStartNotificationIfNeeded(sensorDataDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedSensorData);
    }

    @GetMapping({"/api/v1/sensors/history", "/api/sensor-data/history"})
    public ResponseEntity<Page<SensorDataDto>> getSensorHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(sensorService.getSensorHistory(page, size));
    }

    @GetMapping("/api/acil-mail-test")
    public String acilMailTest() {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return "Mail sender is not configured.";
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(ALERT_EMAIL);
            message.setTo(ALERT_EMAIL);
            message.setSubject("Test Mail");
            message.setText("UYARI: Bu bir test mailidir. Sistem acil durumunda bu maili gonderecektir.");
            mailSender.send(message);
            return "Mail gonderildi. Gmail hesabini kontrol et.";
        } catch (Exception e) {
            return "Mail hatasi: " + e.getMessage();
        }
    }

    private void updateAiDecision(SensorDataDto sensorDataDto) {
        String pythonApiUrl = "http://127.0.0.1:5000/predict";
        Map<String, Object> pythonRequest = Map.of(
                "moisture", sensorDataDto.getMoisturePercent(),
                "is_raining", sensorDataDto.getIsRaining()
        );

        try {
            Map<?, ?> response = restTemplate.postForObject(pythonApiUrl, pythonRequest, Map.class);
            if (response != null && response.get("decision") != null) {
                IrrigationState.lastDecision = response.get("decision").toString();
            }
        } catch (RestClientException ex) {
            IrrigationState.lastDecision = "AI_SERVICE_UNAVAILABLE";
        }
    }

    private void sendPumpStartNotificationIfNeeded(SensorDataDto sensorDataDto) {
        if (!"start".equalsIgnoreCase(sensorDataDto.getPumpState())) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(ALERT_EMAIL);
            message.setTo(ALERT_EMAIL);
            message.setSubject("Smart Irrigation Notification");
            message.setText("Pump will run for " + sensorDataDto.getPumpRemainingTime() + " minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Mail error: " + e.getMessage());
        }
    }
}
