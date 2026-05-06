/*
 * rain_sensor.ino
 * ===============
 * Rain/Snow Detection Module
 *
 * Hardware: Rain/Snow Sensor (e.g., YL-83 / MH-RD)
 * Data Pin: D0 → GPIO34 (Input-only pin — digital read, defined in config.h)
 * Power:    VCC → GPIO14 (Smart Sleep power control — defined in config.h)
 *
 * Responsible: Sude Nur ÖZMEN
 *
 * SMART SLEEP POWER MANAGEMENT:
 *   Instead of connecting the sensor's VCC directly to ESP32's 3.3V rail,
 *   the sensor's VCC is connected to GPIO14. This allows the ESP32 to:
 *     1. Power ON the sensor only when a reading is needed (GPIO14 → HIGH)
 *     2. Wait for sensor warm-up (SENSOR_WARMUP_MS)
 *     3. Read the digital output (D0)
 *     4. Power OFF the sensor after reading (GPIO14 → LOW)
 *
 *   BENEFITS:
 *     • Reduces continuous current draw by ~5-15mA
 *     • Critical for solar-powered / battery systems
 *     • Prevents oxidation/corrosion of the rain sensor pads
 *       (continuous current accelerates corrosion on exposed metal)
 *     • Extends sensor lifespan significantly
 *
 *   NOTE: GPIO14 can source up to ~40mA, which is sufficient for
 *   the rain sensor module (~5-15mA typical draw).
 *
 *   CAUTION about GPIO14: At ESP32 boot, GPIO14 briefly outputs a
 *   PWM signal (used by the JTAG/debug interface). This is harmless
 *   for sensor power — the sensor will see a brief power-on pulse
 *   during boot, then settle to LOW (off) once setup() runs.
 *
 * This module provides functions to detect precipitation.
 * Logic: Most rain sensors output LOW when water is detected
 * and HIGH when the sensor is dry.
 *
 * Functions for main sketch:
 * - rainSensorSetup()    -> Initialize pins (data + power)
 * - rainSensorUpdate()   -> Power on, read, power off, update state
 * - getIsRaining()       -> Returns bool (true if raining)
 * - getRainRawValue()    -> Returns raw 0 or 1
 */

// --- Modül İçi Değişkenler ---
bool  _isRainingInternal = false;
int   _rainRawValueInternal = 1; // Başlangıçta Kuru (HIGH)

/**
 * Sensör pinlerini hazırlar (veri + güç).
 * Pin değerleri config.h dosyasından gelir.
 */
void rainSensorSetup() {
    // --- Configure the sensor POWER pin (Smart Sleep) ---
    // GPIO14 acts as a software-controlled power switch for the sensor
    pinMode(RAIN_POWER_PIN, OUTPUT);
    digitalWrite(RAIN_POWER_PIN, LOW);  // Start with sensor OFF (save power)

    // --- Configure the sensor DATA pin ---
    pinMode(RAIN_SENSOR_PIN, INPUT);

    Serial.println("[RAIN] Modül GPIO " + String(RAIN_SENSOR_PIN) + " üzerinde hazır.");
    Serial.println("[RAIN] Power pin (Smart Sleep): GPIO " + String(RAIN_POWER_PIN));
}

/**
 * Sensörü okur ve durumu günceller.
 * Smart Sleep: Okumadan önce sensörü açar, okuduktan sonra kapatır.
 * Ana loop içerisinde her sensör okuma periyodunda çağrılmalıdır.
 */
void rainSensorUpdate() {
    // --- STEP 1: Power ON the sensor via Smart Sleep pin ---
    digitalWrite(RAIN_POWER_PIN, HIGH);
    delay(SENSOR_WARMUP_MS);  // Allow sensor module to warm up and stabilize

    // --- STEP 2: Read the digital output ---
    // Sensörden dijital okuma yap (0 = Islak, 1 = Kuru)
    _rainRawValueInternal = digitalRead(RAIN_SENSOR_PIN);
    
    // --- STEP 3: Power OFF the sensor to save energy ---
    digitalWrite(RAIN_POWER_PIN, LOW);
    delay(SENSOR_SETTLE_MS);

    // LOW gelirse yağmur var demektir (isRaining = true)
    _isRainingInternal = (_rainRawValueInternal == LOW);
}

// --- Getter Fonksiyonları (Dışarıya veri servis eder) ---

bool getIsRaining() {
    return _isRainingInternal;
}

int getRainRawValue() {
    return _rainRawValueInternal;
}