package com.example.backend.services;

import com.example.backend.domain.dto.IrrigationLogDto;
import com.example.backend.domain.dto.SensorDataDto;
import com.example.backend.domain.entities.SensorData;
import com.example.backend.repositories.SensorDataRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorService.class);

    private final SensorDataRepository sensorDataRepository;
    private final IrrigationService irrigationService;

    @Transactional
    public SensorDataDto saveSensorData(SensorDataDto sensorDataDto) {
        detectAndLogPumpTransition(sensorDataDto.getPumpState(), sensorDataDto.getPumpRemainingTime());

        SensorData sensorData = SensorData.builder()
                .moisturePercent(sensorDataDto.getMoisturePercent())
                .moistureRaw(sensorDataDto.getMoistureRaw())
                .isRaining(sensorDataDto.getIsRaining())
                .rainSensorRaw(sensorDataDto.getRainSensorRaw())
                .pumpState(sensorDataDto.getPumpState())
                .pumpRemainingTime(sensorDataDto.getPumpRemainingTime())
                .timestamp(resolveTimestamp(sensorDataDto.getTimestamp()))
                .build();

        return mapToDto(sensorDataRepository.save(sensorData));
    }

    @Transactional(readOnly = true)
    public Page<SensorDataDto> getSensorHistory(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return sensorDataRepository.findAll(pageRequest).map(this::mapToDto);
    }

    private void detectAndLogPumpTransition(String newState, Integer remainingTimeSecs) {
        if (newState == null) return;

        PageRequest latest = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<SensorData> recent = sensorDataRepository.findAll(latest).getContent();

        String prevState = recent.isEmpty() ? null : recent.get(0).getPumpState();
        boolean wasRunning = isPumpActive(prevState);
        boolean isRunning  = isPumpActive(newState);

        try {
            if (!wasRunning && isRunning) {
                int durationMinutes = remainingTimeSecs != null
                        ? Math.max(1, (int) Math.ceil(remainingTimeSecs / 60.0))
                        : 1;
                irrigationService.logPumpAction(IrrigationLogDto.builder()
                        .pumpStatus("ON")
                        .durationInMinutes(durationMinutes)
                        .timestamp(LocalDateTime.now())
                        .build());
            } else if (wasRunning && !isRunning) {
                irrigationService.logPumpAction(IrrigationLogDto.builder()
                        .pumpStatus("OFF")
                        .durationInMinutes(0)
                        .timestamp(LocalDateTime.now())
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to write irrigation log from sensor transition: {}", e.getMessage());
        }
    }

    private boolean isPumpActive(String state) {
        if (state == null) return false;
        switch (state.trim().toUpperCase()) {
            case "RUNNING": case "ACTIVE": case "ON": case "START": return true;
            default: return false;
        }
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
