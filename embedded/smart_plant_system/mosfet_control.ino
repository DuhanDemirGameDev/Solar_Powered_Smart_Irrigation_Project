/*
 * mosfet_control.ino
 * ==================
 * Water Pump MOSFET Control Module
 * (Replaces the previous relay_control.ino)
 * 
 * Hardware: N-Channel MOSFET (e.g., IRLZ44N, IRF540N, or logic-level MOSFET)
 * Pin:      GATE → GPIO25 (Digital Output from ESP32)
 *           DRAIN → Pump (−) terminal
 *           SOURCE → GND (common ground with ESP32 and power supply)
 * Pump:     (+) terminal → Battery/Solar positive rail
 * 
 * WHY MOSFET INSTEAD OF RELAY?
 *   - No mechanical parts → silent, no click noise
 *   - Much faster switching (nanoseconds vs milliseconds)
 *   - Lower power consumption (no coil to energize)
 *   - No back-EMF issues from relay coil
 *   - Better suited for solar-powered systems
 *   - Can be PWM-controlled for variable pump speed (future)
 * 
 * MOSFET LOGIC (N-Channel):
 *   GATE HIGH → MOSFET conducts → Pump turns ON
 *   GATE LOW  → MOSFET blocks   → Pump turns OFF
 *   (Opposite to typical active-low relay modules)
 * 
 * This file provides all functions related to controlling
 * the water pump through the MOSFET. It implements:
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
 *   - mosfetSetup()              → Initialize MOSFET pin
 *   - mosfetUpdate()             → Must be called every loop (state machine)
 *   - updateMoistureData(pct)    → Feed latest moisture % from sensor module
 *   - updateRainData(raining)    → Feed latest rain status from rain module
 *   - startPump(duration)        → Start pump for N seconds
 *   - startPumpManual(duration)  → Start pump manually (skip moisture auto-stop)
 *   - stopPump(reason)           → Stop pump immediately
 *   - startSmartWatering()       → Auto-determine duration from moisture
 *   - startHeatProtectionBurst() → Short burst for hot weather
 *   - isPumpRunning()            → Check if pump is active
 *   - isPumpInCooldown()         → Check if in cooldown
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
void mosfetSetup() {
    pinMode(MOSFET_PIN, OUTPUT);

    // Ensure pump starts OFF (MOSFET gate LOW = not conducting)
    mosfetOff();
    pumpState = PUMP_IDLE;

    Serial.println("[MOSFET] Initialized on GPIO " + String(MOSFET_PIN));
    Serial.println("[MOSFET] Logic: ACTIVE HIGH (N-Channel MOSFET)");
    Serial.println("[MOSFET] Safety max runtime: " + String(PUMP_MAX_RUNTIME) + "s");
}

// ============================================================
//  Moisture Data Setter
//  Call this from the main sketch after reading the moisture
//  sensor so mosfet_control always has fresh data.
// ============================================================
void updateMoistureData(float moisturePercent) {
    cachedMoisturePercent = moisturePercent;
}

// ============================================================
//  Rain Data Setter
//  Call this from the main sketch after reading the rain
//  sensor so mosfet_control always has fresh data.
// ============================================================
void updateRainData(bool isRaining) {
    cachedIsRaining = isRaining;
}

// ============================================================
//  State Machine — Call Every Loop Iteration
// ============================================================
void mosfetUpdate() {
    switch (pumpState) {
        case PUMP_IDLE:
            // Nothing to do
            break;

        case PUMP_RUNNING: {
            unsigned long elapsed = millis() - pumpStartTime;

            // --- Safety: Absolute max runtime ---
            if (elapsed >= (unsigned long)PUMP_MAX_RUNTIME * 1000UL) {
                Serial.println("[MOSFET] ⚠ SAFETY: Max runtime exceeded. Forcing stop.");
                stopPump("safety_max_runtime");
                return;
            }

            // --- Timer expired ---
            if (elapsed >= pumpDuration) {
                Serial.println("[MOSFET] Timer expired after " + String(elapsed / 1000) + "s.");
                stopPump("timer_expired");
                return;
            }

            // --- Real-time moisture check: stop early if target reached ---
            // ONLY for automatic/smart watering — manual overrides run full duration.
            // Uses cachedMoisturePercent which is updated by the main sketch
            // calling updateMoistureData() each sensor read cycle.
            if (!manualOverride && cachedMoisturePercent >= (float)MOISTURE_THRESHOLD_TARGET) {
                Serial.println("[MOSFET] Target moisture reached (" 
                    + String(cachedMoisturePercent, 1) + "%). Stopping early.");
                stopPump("target_moisture_reached");
                return;
            }

            // --- Progress log every 15 seconds ---
            if (elapsed > 0 && (elapsed % 15000) < 100) {
                Serial.println("[MOSFET] Running... " 
                    + String(elapsed / 1000) + "s / " + String(pumpDuration / 1000) + "s"
                    + " | Moisture: " + String(cachedMoisturePercent, 1) + "%");
            }
            break;
        }

        case PUMP_STOPPING:
            mosfetOff();
            pumpCooldownEnd = millis() + COOLDOWN_PERIOD_MS;
            pumpState = PUMP_COOLDOWN;
            Serial.println("[MOSFET] Cooldown started (" + String(COOLDOWN_PERIOD_MS / 1000) + "s).");
            break;

        case PUMP_COOLDOWN:
            if (millis() >= pumpCooldownEnd) {
                pumpState = PUMP_IDLE;
                Serial.println("[MOSFET] Cooldown complete. Ready.");
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
        Serial.println("[MOSFET] Already running. Ignoring.");
        return false;
    }
    if (pumpState == PUMP_COOLDOWN) {
        Serial.println("[MOSFET] In cooldown. Please wait.");
        return false;
    }
    if (durationSeconds <= 0) {
        Serial.println("[MOSFET] Invalid duration: " + String(durationSeconds));
        return false;
    }

    // Clamp to safety max
    if (durationSeconds > PUMP_MAX_RUNTIME) {
        Serial.println("[MOSFET] Duration clamped to max " + String(PUMP_MAX_RUNTIME) + "s.");
        durationSeconds = PUMP_MAX_RUNTIME;
    }

    // Start
    pumpDuration  = (unsigned long)durationSeconds * 1000UL;
    pumpStartTime = millis();
    pumpCycleCount++;

    mosfetOn();
    pumpState = PUMP_RUNNING;

    Serial.println("[MOSFET] ▶ PUMP STARTED | Duration: " + String(durationSeconds) 
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

    Serial.println("[MOSFET] ■ PUMP STOPPED | Ran: " + String(runtime / 1000) 
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
        Serial.println("[MOSFET] Smart watering declined: Rain detected 🌧️"
            " — rain is watering the plant naturally.");
        return false;
    }

    if (cachedMoisturePercent >= (float)MOISTURE_THRESHOLD_LOW) {
        Serial.println("[MOSFET] Smart watering declined: Moisture " 
            + String(cachedMoisturePercent, 1) + "% (threshold: " 
            + String(MOISTURE_THRESHOLD_LOW) + "%)");
        return false;
    }

    // Determine watering duration based on current moisture level
    int duration = calculateWateringDuration();
    if (duration <= 0) return false;

    Serial.println("[MOSFET] Smart watering: Moisture " 
        + String(cachedMoisturePercent, 1) + "% → " + String(duration) + "s");

    return startPump(duration);
}

// ============================================================
//  Calculate Watering Duration from Moisture Level
//  Thresholds are derived from MOISTURE_THRESHOLD_LOW in config.h
//  so they automatically adapt when the user changes the threshold.
//
//  Example with MOISTURE_THRESHOLD_LOW = 40:
//    0-13%   → 45s (critical — below 1/3 of threshold)
//    13-26%  → 30s (low — below 2/3 of threshold)
//    26-40%  → 15s (moderate — below threshold)
//    ≥40%    → 0 (no watering needed)
// ============================================================
int calculateWateringDuration() {
    float threshold = (float)MOISTURE_THRESHOLD_LOW;
    float tier1 = threshold * 0.33;   // ~1/3 of threshold → critical
    float tier2 = threshold * 0.66;   // ~2/3 of threshold → low

    if (cachedMoisturePercent <= tier1) {
        return PUMP_DURATION_CRITICAL;    // 45s
    } else if (cachedMoisturePercent <= tier2) {
        return PUMP_DURATION_LOW;         // 30s
    } else if (cachedMoisturePercent < threshold) {
        return PUMP_DURATION_MODERATE;    // 15s
    }
    return 0;
}

// ============================================================
//  Heat Protection — Short Burst
// ============================================================
bool startHeatProtectionBurst() {
    Serial.println("[MOSFET] Heat protection burst requested.");
    return startPump(PUMP_DURATION_HEAT_SHORT);
}

// ============================================================
//  Internal MOSFET Control — Gate Drive Logic
//  N-Channel MOSFET: HIGH = ON, LOW = OFF
// ============================================================
void mosfetOn() {
    // N-Channel MOSFET: Drive gate HIGH to turn ON (conduct current)
    digitalWrite(MOSFET_PIN, MOSFET_ACTIVE_HIGH ? HIGH : LOW);
}

void mosfetOff() {
    // N-Channel MOSFET: Drive gate LOW to turn OFF (block current)
    digitalWrite(MOSFET_PIN, MOSFET_ACTIVE_HIGH ? LOW : HIGH);
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
    Serial.println("[MOSFET] Daily counters reset.");
}
