package com.example.backend.services;

import com.example.backend.domain.dto.IrrigationLogDto;
import com.example.backend.domain.entities.IrrigationLog;
import com.example.backend.repositories.IrrigationLogRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IrrigationService {

    private final IrrigationLogRepository irrigationLogRepository;

    @Transactional
    public IrrigationLogDto logPumpAction(IrrigationLogDto irrigationLogDto) {
        IrrigationLog irrigationLog = IrrigationLog.builder()
                .pumpStatus(irrigationLogDto.getPumpStatus().trim().toUpperCase(Locale.ROOT))
                .durationInMinutes(irrigationLogDto.getDurationInMinutes())
                .timestamp(resolveTimestamp(irrigationLogDto.getTimestamp()))
                .build();

        IrrigationLog savedLog = irrigationLogRepository.save(irrigationLog);
        return mapToDto(savedLog);
    }

    @Transactional(readOnly = true)
    public List<IrrigationLogDto> getIrrigationHistory() {
        return irrigationLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"))
                .stream()
                .map(this::mapToDto)
                .toList();
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
