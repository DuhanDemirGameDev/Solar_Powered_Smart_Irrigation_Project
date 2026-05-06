package com.example.backend.services;

import com.example.backend.domain.dto.SensorDataDto;
import com.example.backend.domain.entities.SensorData;
import com.example.backend.repositories.SensorDataRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final SensorDataRepository sensorDataRepository;

    @Transactional
    public SensorDataDto saveSensorData(SensorDataDto sensorDataDto) {
        SensorData sensorData = SensorData.builder()
                .moisturePercent(sensorDataDto.getMoisturePercent())
                .moistureRaw(sensorDataDto.getMoistureRaw())
                .isRaining(sensorDataDto.getIsRaining())
                .rainSensorRaw(sensorDataDto.getRainSensorRaw())
                .pumpState(sensorDataDto.getPumpState())
                .pumpRemainingTime(sensorDataDto.getPumpRemainingTime())
                .timestamp(resolveTimestamp(sensorDataDto.getTimestamp()))
                .build();

        SensorData savedSensorData = sensorDataRepository.save(sensorData);
        return mapToDto(savedSensorData);
    }

    @Transactional(readOnly = true)
    public Page<SensorDataDto> getSensorHistory(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return sensorDataRepository.findAll(pageRequest).map(this::mapToDto);
    }

    private SensorDataDto mapToDto(SensorData sensorData) {
        return SensorDataDto.builder()
                .id(sensorData.getId())
                .moisturePercent(sensorData.getMoisturePercent())
                .moistureRaw(sensorData.getMoistureRaw())
                .isRaining(sensorData.getIsRaining())
                .rainSensorRaw(sensorData.getRainSensorRaw())
                .pumpState(sensorData.getPumpState())
                .pumpRemainingTime(sensorData.getPumpRemainingTime())
                .timestamp(sensorData.getTimestamp())
                .build();
    }

    private LocalDateTime resolveTimestamp(LocalDateTime timestamp) {
        return timestamp != null ? timestamp : LocalDateTime.now();
    }
}
