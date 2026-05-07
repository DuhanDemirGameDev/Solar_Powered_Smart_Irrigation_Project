package com.example.backend.controller;

import com.example.backend.domain.dto.SensorDataDto;
import com.example.backend.services.SensorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Validated
@RestController
public class SensorController {

    private static final String ALERT_EMAIL = "solarpowered0606@gmail.com";
    private static final String AI_PREDICT_URL = "http://127.0.0.1:5000/predict";

    private final SensorService sensorService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RestTemplate restTemplate;

    private long lastEmailSentTime = 0;
    private static final long EMAIL_COOLDOWN_MS = 60000;

    public SensorController(
            SensorService sensorService,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.sensorService = sensorService;
        this.mailSenderProvider = mailSenderProvider;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(3))
                .build();
    }

    @PostMapping({"/api/v1/sensors", "/api/sensor-data"})
    public ResponseEntity<SensorDataDto> createSensorData(@Valid @RequestBody SensorDataDto sensorDataDto) {
        SensorDataDto savedSensorData = sensorService.saveSensorData(sensorDataDto);

        String aiDecision = updateAiDecision(sensorDataDto);
        
        boolean isManualStart = "start".equalsIgnoreCase(sensorDataDto.getPumpState()) 
                             || "on".equalsIgnoreCase(sensorDataDto.getPumpState());
        boolean isAiStart = "IRRIGATE".equalsIgnoreCase(aiDecision);

        if (isManualStart || isAiStart) {
            sendPumpStartNotificationIfNeeded();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedSensorData);
    }

    @GetMapping({"/api/v1/sensors/history", "/api/sensor-data/history"})
    public ResponseEntity<Page<SensorDataDto>> getSensorHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
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

    private String updateAiDecision(SensorDataDto sensorDataDto) {
        Map<String, Object> pythonRequest = Map.of(
                "moisture", sensorDataDto.getMoisturePercent(),
                "is_raining", sensorDataDto.getIsRaining()
        );

        String decision = "AI_SERVICE_UNAVAILABLE";

        try {
            Map<?, ?> response = restTemplate.postForObject(AI_PREDICT_URL, pythonRequest, Map.class);
            if (response != null && response.get("decision") != null) {
                decision = response.get("decision").toString();
            }
        } catch (RestClientException ex) {
            decision = "AI_SERVICE_UNAVAILABLE";
        }

        synchronized (IrrigationState.COMMAND_LOCK) {
            IrrigationState.lastDecision = decision;
        }
        
        System.out.println("AI Karari: " + decision);
        return decision;
    }

    private void sendPumpStartNotificationIfNeeded() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastEmailSentTime <= EMAIL_COOLDOWN_MS) {
            System.out.println("Pompa calisiyor ama mail spamini onlemek icin beklemede kalindi.");
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
            message.setSubject("Akilli Sulama Bildirimi");
            message.setText("Sistem acil durumu! Nem dustu veya manuel komut verildi. Pompa calistiriliyor.");
            mailSender.send(message);
            
            System.out.println("MAIL KUTUYA DUSTU!");
            lastEmailSentTime = currentTime;
        } catch (Exception e) {
            System.err.println("Mail Hatasi: " + e.getMessage());
        }
    }
}