package com.example.backend.controller;

import com.example.backend.domain.dto.ApiErrorResponse;
import com.example.backend.domain.dto.IrrigationLogDto;
import com.example.backend.domain.dto.PumpCommandDto;
import com.example.backend.services.IrrigationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/irrigation")
@RequiredArgsConstructor
public class IrrigationController {

    private static final int DEFAULT_IRRIGATION_DURATION_SECONDS = 15;

    private final IrrigationService irrigationService;

    @GetMapping("/command")
    public ResponseEntity<Map<String, Object>> getPumpCommand() {
        synchronized (IrrigationState.COMMAND_LOCK) {
            if (hasPendingCommand()) {
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
                return ResponseEntity.ok(buildCommandResponse(
                        "start",
                        DEFAULT_IRRIGATION_DURATION_SECONDS,
                        "AI decision: IRRIGATE"
                ));
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
        int durationSeconds = resolveDurationSeconds(action, pumpCommandDto.getDuration());
        String reason = resolveReason(pumpCommandDto.getReason(), action);

        synchronized (IrrigationState.COMMAND_LOCK) {
            if ("none".equals(action)) {
                clearPendingCommand();
                IrrigationState.lastDecision = "IDLE";
                return ResponseEntity.ok(buildIdleResponse("Manual command cleared"));
            }

            IrrigationState.pendingAction = action;
            IrrigationState.pendingDuration = durationSeconds;
            IrrigationState.pendingReason = reason;
        }

        return ResponseEntity.ok(Map.of(
                "queued", true,
                "action", action,
                "duration", durationSeconds,
                "unit", "seconds",
                "reason", reason
        ));
    }

    @PostMapping("/manual")
    public ResponseEntity<Map<String, String>> triggerManualIrrigation() {
        synchronized (IrrigationState.COMMAND_LOCK) {
            IrrigationState.pendingAction = "start";
            IrrigationState.pendingDuration = DEFAULT_IRRIGATION_DURATION_SECONDS;
            IrrigationState.pendingReason = "Manual irrigation endpoint";
        }

        return ResponseEntity.ok(Map.of("message", "Manual irrigation command queued"));
    }

    @PostMapping("/log")
    public ResponseEntity<IrrigationLogDto> logPumpAction(@Valid @RequestBody IrrigationLogDto irrigationLogDto) {
        IrrigationLogDto savedLog = irrigationService.logPumpAction(irrigationLogDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLog);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<IrrigationLogDto>> getIrrigationHistory(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(irrigationService.getIrrigationHistory(page, size));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                validationErrors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    private boolean hasPendingCommand() {
        return !"none".equals(IrrigationState.pendingAction);
    }

    private Map<String, Object> buildCommandResponse(String action, int durationSeconds, String reason) {
        return Map.of(
                "hasCommand", true,
                "action", action,
                "duration", durationSeconds,
                "unit", "seconds",
                "reason", reason
        );
    }

    private Map<String, Object> buildIdleResponse(String reason) {
        return Map.of(
                "hasCommand", false,
                "action", "none",
                "duration", 0,
                "unit", "seconds",
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

    private int resolveDurationSeconds(String action, Integer durationSeconds) {
        if (!"start".equals(action)) {
            return 0;
        }

        return durationSeconds == null || durationSeconds <= 0
                ? DEFAULT_IRRIGATION_DURATION_SECONDS
                : durationSeconds;
    }

    private String resolveReason(String reason, String action) {
        if (reason != null && !reason.isBlank()) {
            return reason.trim();
        }

        return "Manual override: " + action;
    }
}
