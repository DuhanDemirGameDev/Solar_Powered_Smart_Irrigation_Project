package com.example.backend.controller;

import com.example.backend.domain.dto.SensorDataDto;
import com.example.backend.services.SensorService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.web.client.RestTemplate;

@RestController
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;
    private final JavaMailSender mailSender; // SADECE SENİN MAİL MOTORUNU EKLEDİK
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping({"/api/v1/sensors", "/api/sensor-data"})
    public ResponseEntity<SensorDataDto> createSensorData(@Valid @RequestBody SensorDataDto sensorDataDto) {
        
        // 1. SENSÖR KAYDI (Arkadaşlarının kodu - Dokunulmadı)
        SensorDataDto savedSensorData = sensorService.saveSensorData(sensorDataDto);

        // 2. YAPAY ZEKA BAĞLANTISI (Berke'nin/Arkadaşlarının kodu - Aynen geri geldi)
        try {
            String pythonApiUrl = "http://127.0.0.1:5000/predict";
            Map<String, Object> pythonRequest = Map.of(
                    "moisture", sensorDataDto.getMoisturePercent(),
                    "is_raining", sensorDataDto.getIsRaining()
            );

            Map response = restTemplate.postForObject(pythonApiUrl, pythonRequest, Map.class);
            if (response != null && response.get("decision") != null) {
                System.out.println("AI Karari: " + response.get("decision"));
            }
        } catch (Exception e) {
            System.err.println("AI Servisine ulasilamadi: " + e.getMessage());
        }

        // 3. SENİN GÖREVİN: MAİL FIRLATMA (Araya sessizce eklendi)
        if ("start".equalsIgnoreCase(sensorDataDto.getPumpState())) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("solarpower0606@gmail.com");
                message.setTo("solarpower0606@gmail.com");
                message.setSubject("🌱 Akıllı Sulama Bildirimi");
                message.setText("Sistem acil durumu! Pompa " + sensorDataDto.getPumpRemainingTime() + " dakika calisacak.");
                
                mailSender.send(message);
                System.out.println("✅ MAIL KUTUYA DUSTU!");
            } catch (Exception e) {
                System.err.println("❌ Mail Hatasi: " + e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedSensorData);
    }

    // GEÇMİŞİ LİSTELEME (Arkadaşlarının kodu - Aynen geri geldi)
    @GetMapping({"/api/v1/sensors/history", "/api/sensor-data/history"})
    public ResponseEntity<Page<SensorDataDto>> getSensorHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(sensorService.getSensorHistory(page, size));
    }
    @GetMapping("/api/acil-mail-test")
    public String acilMailTest() {
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setFrom("solarpower0606@gmail.com");
            message.setTo("solarpower0606@gmail.com");
            message.setSubject("TEST MAİLİ");
            message.setText("UYARI: Bu bir test mailidir. Sistem acil durumunda bu maili gönderecektir.");
            mailSender.send(message);
            return "MAİL GÖNDERİLDİ! Gidip Gmail'ini kontrol et.";
        } catch (Exception e) {
            return "HATA ÇIKTI: " + e.getMessage();
        }
    }
}