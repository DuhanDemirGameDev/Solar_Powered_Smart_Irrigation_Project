// moisture_sensor.ino
// Artık config.h dahil olduğu için tanımlamalara gerek yok
// 
// FIX: Multiple-sample averaging + validation to prevent
//      bogus Raw:0 (100%) readings from the capacitive sensor.

float currentMoisturePercent = 0.0;
int   currentRawValue = 0;

// Number of ADC samples to average per reading
#define MOISTURE_SAMPLE_COUNT   10
// Minimum valid ADC value (below this = sensor not connected or glitch)
#define MOISTURE_RAW_MIN        300
// Maximum valid ADC value
#define MOISTURE_RAW_MAX        4095

void moistureSensorSetup() {
    analogReadResolution(12);
    // Set attenuation for full 0-3.3V range on the ADC pin
    analogSetPinAttenuation(MOISTURE_SENSOR_PIN, ADC_11db);
    pinMode(MOISTURE_SENSOR_PIN, INPUT);

    // Take a few throwaway reads to stabilize the ADC
    for (int i = 0; i < 5; i++) {
        analogRead(MOISTURE_SENSOR_PIN);
        delay(10);
    }
    Serial.println("[MOISTURE] Initialized on GPIO " + String(MOISTURE_SENSOR_PIN));
}

void moistureSensorUpdate() {
    // --- Multi-sample averaging with outlier rejection ---
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