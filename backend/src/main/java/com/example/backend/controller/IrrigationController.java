package com.example.backend.controller;

import com.example.backend.domain.dto.IrrigationLogDto;
import com.example.backend.services.IrrigationService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/irrigation")
@RequiredArgsConstructor
public class IrrigationController {

    private final IrrigationService irrigationService;

    @GetMapping({"/command", "/api/pump-command"})
    public ResponseEntity<Map<String, Object>> getPumpCommand() {

        String decision = IrrigationState.lastDecision;

        if ("IRRIGATE".equals(decision)) {
            return ResponseEntity.ok(Map.of(
                    "hasCommand", true,
                    "action", "start",
                    "duration", 15,
                    "reason", "AI decision: IRRIGATE"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "hasCommand", false,
                "action", "none",
                "duration", 0,
                "reason", "AI decision: " + decision
        ));
    }

    @PostMapping("/log")
    public ResponseEntity<IrrigationLogDto> logPumpAction(@Valid @RequestBody IrrigationLogDto irrigationLogDto) {
        IrrigationLogDto savedLog = irrigationService.logPumpAction(irrigationLogDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLog);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<IrrigationLogDto>> getIrrigationHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(irrigationService.getIrrigationHistory(page, size));
    }

}
