/*
 * relay_control.ino
 * =================
 * Water Pump Relay Control Module
 * 
 * Hardware: 1-Channel Relay Module + Water Pump
 * Pin:      IN → GPIO 26 (Digital Output)
 * Power:    DC+ → Battery | DC- → Common Ground
 * 
 * Responsible: [Your Name]
 * 
 * This file provides all functions related to controlling
 * the water pump through the relay module. It implements:
 *   - Timed watering with automatic shutoff
 *   - Real-time moisture monitoring during watering (auto-stop when target reached)
 *   - Smart watering (duration based on moisture level)
 *   - Heat protection short burst
 *   - Safety limits (max runtime, cooldown between cycles)
 * 
 * NOTE: This module does NOT own the moisture sensor.
 *       It receives moisture data from the main sketch via setter functions.
 *       The moisture sensor module is maintained by a teammate.
 * 
 * Other modules can call these functions:
 *   - relaySetup()               → Initialize relay pin
 *   - relayUpdate()              → Must be called every loop (state machine)
 *   - updateMoistureData(pct)    → Feed latest moisture % from sensor module
 *   - startPump(duration)        → Start pump for N seconds
 *   - stopPump(reason)           → Stop pump immediately
 *   - startSmartWatering()       → Auto-determine duration from moisture
 *   - startHeatProtectionBurst() → Short burst for hot weather
 *   - isPumpRunning()            → Check if pump is active
 *   - getPumpStateString()       → Get state as text
 *   - getPumpRemainingTime()     → Seconds remaining
 */

// ============================================================
//  Pump State Machine
// ============================================================
enum PumpState {
    PUMP_IDLE,
    PUMP_RUNNING,
    PUMP_STOPPING,
    PUMP_COOLDOWN
};

#define COOLDOWN_PERIOD_MS  10000   // 10 seconds between pump cycles

PumpState     pumpState            = PUMP_IDLE;
unsigned long pumpStartTime        = 0;
unsigned long pumpDuration         = 0;       // milliseconds
unsigned long pumpCooldownEnd      = 0;
unsigned long pumpTotalRunToday    = 0;       // seconds
int           pumpCycleCount       = 0;
String        pumpLastReason       = "";
bool          manualOverride       = false;   // true = skip moisture auto-stop

// ============================================================
//  Moisture Data Cache
//  (Fed from outside — the moisture sensor module provides
//   the readings, the main sketch passes them here via
//   updateMoistureData().)
// ============================================================
float  cachedMoisturePercent = 0.0;

// ============================================================
//  Rain Data Cache
//  (Fed from outside — the rain sensor module provides
//   the readings, the main sketch passes them here via
//   updateRainData().)
// ============================================================
bool   cachedIsRaining = false;

// ============================================================
//  Initialization
// ============================================================
void relaySetup() {
    pinMode(RELAY_PIN, OUTPUT);

    // Ensure pump starts OFF
    relayOff();
    pumpState = PUMP_IDLE;

    Serial.println("[RELAY] Initialized on GPIO " + String(RELAY_PIN));
    Serial.println("[RELAY] Logic: " + String(RELAY_ACTIVE_LOW ? "ACTIVE LOW" : "ACTIVE HIGH"));
    Serial.println("[RELAY] Safety max runtime: " + String(PUMP_MAX_RUNTIME) + "s");
}

// ============================================================
//  Moisture Data Setter
//  Call this from the main sketch after reading the moisture
//  sensor so relay_control always has fresh data.
// ============================================================
void updateMoistureData(float moisturePercent) {
    cachedMoisturePercent = moisturePercent;
}

// ============================================================
//  Rain Data Setter
//  Call this from the main sketch after reading the rain
//  sensor so relay_control always has fresh data.
// ============================================================
void updateRainData(bool isRaining) {
    cachedIsRaining = isRaining;
}

// ============================================================
//  State Machine — Call Every Loop Iteration
// ============================================================
void relayUpdate() {
    switch (pumpState) {
        case PUMP_IDLE:
            // Nothing to do
            break;

        case PUMP_RUNNING: {
            unsigned long elapsed = millis() - pumpStartTime;

            // --- Safety: Absolute max runtime ---
            if (elapsed >= (unsigned long)PUMP_MAX_RUNTIME * 1000UL) {
                Serial.println("[RELAY] ⚠ SAFETY: Max runtime exceeded. Forcing stop.");
                stopPump("safety_max_runtime");
                return;
            }

            // --- Timer expired ---
            if (elapsed >= pumpDuration) {
                Serial.println("[RELAY] Timer expired after " + String(elapsed / 1000) + "s.");
                stopPump("timer_expired");
                return;
            }

            // --- Real-time moisture check: stop early if target reached ---
            // ONLY for automatic/smart watering — manual overrides run full duration.
            // Uses cachedMoisturePercent which is updated by the main sketch
            // calling updateMoistureData() each sensor read cycle.
            if (!manualOverride && cachedMoisturePercent >= (float)MOISTURE_THRESHOLD_TARGET) {
                Serial.println("[RELAY] Target moisture reached (" 
                    + String(cachedMoisturePercent, 1) + "%). Stopping early.");
                stopPump("target_moisture_reached");
                return;
            }

            // --- Progress log every 15 seconds ---
            if (elapsed > 0 && (elapsed % 15000) < 100) {
                Serial.println("[RELAY] Running... " 
                    + String(elapsed / 1000) + "s / " + String(pumpDuration / 1000) + "s"
                    + " | Moisture: " + String(cachedMoisturePercent, 1) + "%");
            }
            break;
        }

        case PUMP_STOPPING:
            relayOff();
            pumpCooldownEnd = millis() + COOLDOWN_PERIOD_MS;
            pumpState = PUMP_COOLDOWN;
            Serial.println("[RELAY] Cooldown started (" + String(COOLDOWN_PERIOD_MS / 1000) + "s).");
            break;

        case PUMP_COOLDOWN:
            if (millis() >= pumpCooldownEnd) {
                pumpState = PUMP_IDLE;
                Serial.println("[RELAY] Cooldown complete. Ready.");
            }
            break;
    }
}

// ============================================================
//  Start Pump for a Specific Duration (seconds)
// ============================================================
bool startPump(int durationSeconds) {
    // Validate state
    if (pumpState == PUMP_RUNNING) {
        Serial.println("[RELAY] Already running. Ignoring.");
        return false;
    }
    if (pumpState == PUMP_COOLDOWN) {
        Serial.println("[RELAY] In cooldown. Please wait.");
        return false;
    }
    if (durationSeconds <= 0) {
        Serial.println("[RELAY] Invalid duration: " + String(durationSeconds));
        return false;
    }

    // Clamp to safety max
    if (durationSeconds > PUMP_MAX_RUNTIME) {
        Serial.println("[RELAY] Duration clamped to max " + String(PUMP_MAX_RUNTIME) + "s.");
        durationSeconds = PUMP_MAX_RUNTIME;
    }

    // Start
    pumpDuration  = (unsigned long)durationSeconds * 1000UL;
    pumpStartTime = millis();
    pumpCycleCount++;

    relayOn();
    pumpState = PUMP_RUNNING;

    Serial.println("[RELAY] ▶ PUMP STARTED | Duration: " + String(durationSeconds) 
        + "s | Cycle #" + String(pumpCycleCount)
        + " | Mode: " + String(manualOverride ? "MANUAL" : "AUTO"));

    return true;
}

// ============================================================
//  Start Pump Manually (from web panel / backend command)
//  Skips moisture-based early stop — runs for the full duration.
// ============================================================
bool startPumpManual(int durationSeconds) {
    manualOverride = true;
    return startPump(durationSeconds);
}

// ============================================================
//  Stop Pump Immediately
// ============================================================
void stopPump(const char* reason) {
    if (pumpState != PUMP_RUNNING && pumpState != PUMP_STOPPING) {
        return;
    }

    unsigned long runtime = millis() - pumpStartTime;
    pumpTotalRunToday += runtime / 1000;
    pumpLastReason = String(reason);
    manualOverride = false;  // Reset override on stop

    Serial.println("[RELAY] ■ PUMP STOPPED | Ran: " + String(runtime / 1000) 
        + "s | Reason: " + String(reason)
        + " | Total today: " + String(pumpTotalRunToday) + "s");

    pumpState = PUMP_STOPPING;
}

// ============================================================
//  Smart Watering — Duration Auto-Calculated from Moisture
//  Uses the cached moisture data fed via updateMoistureData().
// ============================================================
bool startSmartWatering() {
    // --- Rain Check: If it's raining, skip auto-watering ---
    if (cachedIsRaining) {
        Serial.println("[RELAY] Smart watering declined: Rain detected 🌧️"
            " — rain is watering the plant naturally.");
        return false;
    }

    if (cachedMoisturePercent >= (float)MOISTURE_THRESHOLD_LOW) {
        Serial.println("[RELAY] Smart watering declined: Moisture " 
            + String(cachedMoisturePercent, 1) + "% (threshold: " 
            + String(MOISTURE_THRESHOLD_LOW) + "%)");
        return false;
    }

    // Determine watering duration based on current moisture level
    int duration = calculateWateringDuration();
    if (duration <= 0) return false;

    Serial.println("[RELAY] Smart watering: Moisture " 
        + String(cachedMoisturePercent, 1) + "% → " + String(duration) + "s");

    return startPump(duration);
}

// ============================================================
//  Calculate Watering Duration from Moisture Level
//    0-10%  → 3 minutes (critical)
//    10-20% → 2 minutes (low)
//    20-30% → 1 minute  (moderate)
//    >30%   → 0 (no watering needed)
// ============================================================
int calculateWateringDuration() {
    if (cachedMoisturePercent <= 10.0) {
        return PUMP_DURATION_CRITICAL;    // 180s
    } else if (cachedMoisturePercent <= 20.0) {
        return PUMP_DURATION_LOW;         // 120s
    } else if (cachedMoisturePercent < (float)MOISTURE_THRESHOLD_LOW) {
        return PUMP_DURATION_MODERATE;    // 60s
    }
    return 0;
}

// ============================================================
//  Heat Protection — Short Burst
// ============================================================
bool startHeatProtectionBurst() {
    Serial.println("[RELAY] Heat protection burst requested.");
    return startPump(PUMP_DURATION_HEAT_SHORT);
}

// ============================================================
//  Internal Relay Control — LOW/HIGH Logic
// ============================================================
void relayOn() {
    digitalWrite(RELAY_PIN, RELAY_ACTIVE_LOW ? LOW : HIGH);
}

void relayOff() {
    digitalWrite(RELAY_PIN, RELAY_ACTIVE_LOW ? HIGH : LOW);
}

// ============================================================
//  Status Getters — Used by WiFi Module & Main Sketch
// ============================================================

bool isPumpRunning() {
    return pumpState == PUMP_RUNNING;
}

bool isPumpInCooldown() {
    return pumpState == PUMP_COOLDOWN;
}

const char* getPumpStateString() {
    switch (pumpState) {
        case PUMP_IDLE:     return "IDLE";
        case PUMP_RUNNING:  return "RUNNING";
        case PUMP_STOPPING: return "STOPPING";
        case PUMP_COOLDOWN: return "COOLDOWN";
        default:            return "UNKNOWN";
    }
}

int getPumpRemainingTime() {
    if (pumpState != PUMP_RUNNING) return 0;
    unsigned long elapsed = millis() - pumpStartTime;
    if (elapsed >= pumpDuration) return 0;
    return (pumpDuration - elapsed) / 1000;
}

int getPumpElapsedTime() {
    if (pumpState != PUMP_RUNNING) return 0;
    return (millis() - pumpStartTime) / 1000;
}

void resetPumpDailyCounters() {
    pumpTotalRunToday = 0;
    pumpCycleCount = 0;
    Serial.println("[RELAY] Daily counters reset.");
}
