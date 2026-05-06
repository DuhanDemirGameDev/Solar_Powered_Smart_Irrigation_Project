// moisture_sensor.ino
// ====================
// Capacitive Soil Moisture Sensor Module
//
// Hardware: Capacitive Soil Moisture Sensor v1.2 (or similar)
// Data Pin: AOUT → GPIO35 (ADC1_CH7, input-only — defined in config.h)
// Power:    VCC  → GPIO4  (Smart Sleep power control — defined in config.h)
//
// SMART SLEEP POWER MANAGEMENT:
//   Instead of connecting the sensor's VCC directly to ESP32's 3.3V rail,
//   the sensor's VCC is connected to GPIO4. This allows the ESP32 to:
//     1. Power ON the sensor only when a reading is needed (GPIO4 → HIGH)
//     2. Wait for sensor warm-up (SENSOR_WARMUP_MS)
//     3. Take multi-sample averaged reading
//     4. Power OFF the sensor after reading (GPIO4 → LOW)
//
//   BENEFITS:
//     • Reduces continuous current draw by ~5-10mA per sensor
//     • Critical for solar-powered / battery systems
//     • Extends sensor lifespan (capacitive sensors degrade
//       faster under continuous voltage)
//     • Prevents electrolysis on exposed sensor pads
//
//   NOTE: GPIO4 can source up to ~40mA, which is more than enough
//   for a typical capacitive moisture sensor (~5-10mA draw).
//
// FIX: Multi-sample averaging + validation to prevent
//      bogus Raw:0 (100%) readings from the capacitive sensor.

float currentMoisturePercent = 0.0;
int   currentRawValue = 0;

// Number of ADC samples to average per reading
#define MOISTURE_SAMPLE_COUNT   10
// Minimum valid ADC value (below this = sensor not connected or glitch)
// Sensor's wet-end minimum is ~250, so 200 gives margin for valid reads
#define MOISTURE_RAW_MIN        200
// Maximum valid ADC value (sensor's dry-end max is ~2700)
// Anything above 2800 is likely a sensor fault or disconnection
#define MOISTURE_RAW_MAX        2800

void moistureSensorSetup() {
    // --- Configure the sensor POWER pin (Smart Sleep) ---
    // GPIO4 acts as a software-controlled power switch for the sensor
    pinMode(MOISTURE_POWER_PIN, OUTPUT);
    digitalWrite(MOISTURE_POWER_PIN, LOW);  // Start with sensor OFF (save power)

    // --- Configure the sensor DATA pin ---
    analogReadResolution(12);
    // Set attenuation for full 0-3.3V range on the ADC pin
    analogSetPinAttenuation(MOISTURE_SENSOR_PIN, ADC_11db);
    pinMode(MOISTURE_SENSOR_PIN, INPUT);

    // --- Initial stabilization read (power on briefly) ---
    digitalWrite(MOISTURE_POWER_PIN, HIGH);   // Power ON sensor
    delay(SENSOR_WARMUP_MS);                   // Wait for sensor to stabilize

    // Take a few throwaway reads to stabilize the ADC
    for (int i = 0; i < 5; i++) {
        analogRead(MOISTURE_SENSOR_PIN);
        delay(10);
    }

    digitalWrite(MOISTURE_POWER_PIN, LOW);    // Power OFF sensor after init
    delay(SENSOR_SETTLE_MS);

    Serial.println("[MOISTURE] Initialized on GPIO " + String(MOISTURE_SENSOR_PIN));
    Serial.println("[MOISTURE] Power pin (Smart Sleep): GPIO " + String(MOISTURE_POWER_PIN));
}

void moistureSensorUpdate() {
    // --- STEP 1: Power ON the sensor via Smart Sleep pin ---
    digitalWrite(MOISTURE_POWER_PIN, HIGH);
    delay(SENSOR_WARMUP_MS);  // Allow sensor to warm up and stabilize

    // --- STEP 2: Multi-sample averaging with outlier rejection ---
    long sum = 0;
    int  validSamples = 0;

    for (int i = 0; i < MOISTURE_SAMPLE_COUNT; i++) {
        int sample = analogRead(MOISTURE_SENSOR_PIN);
        delay(5);  // Small delay between ADC reads for stability

        // Only count samples within the valid range
        if (sample >= MOISTURE_RAW_MIN && sample <= MOISTURE_RAW_MAX) {
            sum += sample;
            validSamples++;
        }
    }

    // --- STEP 3: Power OFF the sensor to save energy ---
    digitalWrite(MOISTURE_POWER_PIN, LOW);
    delay(SENSOR_SETTLE_MS);

    if (validSamples > 0) {
        currentRawValue = sum / validSamples;
    } else {
        // All samples were invalid — keep previous value, log warning
        Serial.println("[MOISTURE] ⚠ All " + String(MOISTURE_SAMPLE_COUNT)
            + " samples invalid (below " + String(MOISTURE_RAW_MIN)
            + "). Keeping previous value: " + String(currentRawValue));
        return;  // Don't update percentage with bad data
    }

    // --- Map raw ADC value to percentage ---
    // MOISTURE_AIR_VALUE   = dry sensor in air (high ADC → 0%)
    // MOISTURE_WATER_VALUE = sensor in water   (low ADC  → 100%)
    // Using float math instead of integer map() for better precision
    float rawF = (float)currentRawValue;
    float airF = (float)MOISTURE_AIR_VALUE;
    float watF = (float)MOISTURE_WATER_VALUE;

    currentMoisturePercent = ((airF - rawF) / (airF - watF)) * 100.0;

    // Clamp to 0-100 range
    if (currentMoisturePercent < 0.0)   currentMoisturePercent = 0.0;
    if (currentMoisturePercent > 100.0) currentMoisturePercent = 100.0;

    Serial.println("[MOISTURE] Raw: " + String(currentRawValue)
        + " → " + String(currentMoisturePercent, 1) + "%"
        + " (valid samples: " + String(validSamples) + "/" + String(MOISTURE_SAMPLE_COUNT) + ")");
}

float getMoisturePercentage() { return currentMoisturePercent; }
int getMoistureRaw() { return currentRawValue; }