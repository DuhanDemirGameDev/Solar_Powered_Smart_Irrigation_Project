package com.example.backend.controller;

import com.example.backend.domain.dto.IrrigationLogDto;
import com.example.backend.services.IrrigationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/irrigation")
@RequiredArgsConstructor
public class IrrigationController {

    private final IrrigationService irrigationService;

    @PostMapping("/log")
    public ResponseEntity<IrrigationLogDto> logPumpAction(@Valid @RequestBody IrrigationLogDto irrigationLogDto) {
        IrrigationLogDto savedLog = irrigationService.logPumpAction(irrigationLogDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLog);
    }

    @GetMapping("/history")
    public ResponseEntity<List<IrrigationLogDto>> getIrrigationHistory() {
        return ResponseEntity.ok(irrigationService.getIrrigationHistory());
    }
}
