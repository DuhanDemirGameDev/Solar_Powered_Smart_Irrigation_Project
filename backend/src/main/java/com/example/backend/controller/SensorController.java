package com.example.backend.controller;

import com.example.backend.domain.dto.SensorDataDto;
import com.example.backend.services.SensorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;

    @PostMapping({"/api/v1/sensors", "/api/sensor-data"})
    public ResponseEntity<SensorDataDto> createSensorData(@Valid @RequestBody SensorDataDto sensorDataDto) {
        SensorDataDto savedSensorData = sensorService.saveSensorData(sensorDataDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSensorData);
    }

    @GetMapping({"/api/v1/sensors/history", "/api/sensor-data/history"})
    public ResponseEntity<Page<SensorDataDto>> getSensorHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(sensorService.getSensorHistory(page, size));
    }
}
