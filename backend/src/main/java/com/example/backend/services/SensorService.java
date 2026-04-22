package com.example.backend.services;

import com.example.backend.domain.dto.SensorDataDto;
import com.example.backend.domain.entities.SensorData;
import com.example.backend.repositories.SensorDataRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorDataRepository sensorDataRepository;

    @Transactional
    public SensorDataDto saveSensorData(SensorDataDto sensorDataDto) {
        SensorData sensorData = SensorData.builder()
                .soilMoistureLevel(sensorDataDto.getSoilMoistureLevel())
                .isRaining(sensorDataDto.getIsRaining())
                .timestamp(resolveTimestamp(sensorDataDto.getTimestamp()))
                .build();

        SensorData savedSensorData = sensorDataRepository.save(sensorData);
        return mapToDto(savedSensorData);
    }

    @Transactional(readOnly = true)
    public List<SensorDataDto> getSensorHistory() {
        return sensorDataRepository.findTop10ByOrderByTimestampDesc()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private SensorDataDto mapToDto(SensorData sensorData) {
        return SensorDataDto.builder()
                .id(sensorData.getId())
                .soilMoistureLevel(sensorData.getSoilMoistureLevel())
                .isRaining(sensorData.getIsRaining())
                .timestamp(sensorData.getTimestamp())
                .build();
    }

    private LocalDateTime resolveTimestamp(LocalDateTime timestamp) {
        return timestamp != null ? timestamp : LocalDateTime.now();
    }
}
