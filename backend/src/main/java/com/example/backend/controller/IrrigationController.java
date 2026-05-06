package com.example.backend.controller;

import com.example.backend.domain.dto.IrrigationLogDto;
import com.example.backend.domain.dto.PumpCommandDto;
import com.example.backend.services.IrrigationService;
import jakarta.validation.Valid;
import java.util.Locale;
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

    @GetMapping("/command")
    public ResponseEntity<Map<String, Object>> getPumpCommand() {
        synchronized (IrrigationState.COMMAND_LOCK) {
            if (hasPendingManualCommand()) {
                Map<String, Object> command = buildCommandResponse(
                        IrrigationState.pendingAction,
                        IrrigationState.pendingDuration,
                        IrrigationState.pendingReason
                );
                clearPendingCommand();
                return ResponseEntity.ok(command);
            }

            String decision = normalizeDecision(IrrigationState.lastDecision);

            if ("IRRIGATE".equals(decision)) {
                IrrigationState.lastDecision = "IDLE";
                return ResponseEntity.ok(buildCommandResponse("start", 15, "AI decision: IRRIGATE"));
            }

            if ("STOP".equals(decision)) {
                IrrigationState.lastDecision = "IDLE";
                return ResponseEntity.ok(buildCommandResponse("stop", 0, "AI decision: STOP"));
            }

            return ResponseEntity.ok(buildIdleResponse("AI decision: " + IrrigationState.lastDecision));
        }
    }

    @PostMapping("/set-command")
    public ResponseEntity<Map<String, Object>> setManualCommand(@Valid @RequestBody PumpCommandDto pumpCommandDto) {
        String action = normalizeAction(pumpCommandDto.getAction());
        int duration = resolveDuration(action, pumpCommandDto.getDuration());
        String reason = resolveReason(pumpCommandDto.getReason(), action);

        synchronized (IrrigationState.COMMAND_LOCK) {
            if ("none".equals(action)) {
                clearPendingCommand();
                IrrigationState.lastDecision = "IDLE";
                return ResponseEntity.ok(buildIdleResponse("Manual command cleared"));
            }

            IrrigationState.pendingAction = action;
            IrrigationState.pendingDuration = duration;
            IrrigationState.pendingReason = reason;
        }

        return ResponseEntity.ok(Map.of(
                "queued", true,
                "action", action,
                "duration", duration,
                "reason", reason
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

    private boolean hasPendingManualCommand() {
        return !"none".equals(IrrigationState.pendingAction);
    }

    private Map<String, Object> buildCommandResponse(String action, int duration, String reason) {
        return Map.of(
                "hasCommand", true,
                "action", action,
                "duration", duration,
                "reason", reason
        );
    }

    private Map<String, Object> buildIdleResponse(String reason) {
        return Map.of(
                "hasCommand", false,
                "action", "none",
                "duration", 0,
                "reason", reason
        );
    }

    private void clearPendingCommand() {
        IrrigationState.pendingAction = "none";
        IrrigationState.pendingDuration = 0;
        IrrigationState.pendingReason = "No pending command";
    }

    private String normalizeAction(String action) {
        return action.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDecision(String decision) {
        return decision == null ? "IDLE" : decision.trim().toUpperCase(Locale.ROOT);
    }

    private int resolveDuration(String action, Integer duration) {
        if (!"start".equals(action)) {
            return 0;
        }

        return duration == null || duration <= 0 ? 15 : duration;
    }

    private String resolveReason(String reason, String action) {
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }

        return "Manual override: " + action;
    }

}
