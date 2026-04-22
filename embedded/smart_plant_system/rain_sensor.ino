/*
 * rain_sensor.ino
 * ===============
 * Rain/Snow Detection Module
 * * Hardware: Rain/Snow Sensor (e.g., YL-83 / MH-RD)
 * Pin:      D0 -> GPIO 33 (Digital Input - Defined in config.h)
 * * Responsible: Sude Nur ÖZMEN
 * * This module provides functions to detect precipitation.
 * Logic: Most rain sensors output LOW when water is detected 
 * and HIGH when the sensor is dry.
 * * Functions for main sketch:
 * - rainSensorSetup()    -> Initialize pin
 * - rainSensorUpdate()   -> Read and update internal state
 * - getIsRaining()       -> Returns bool (true if raining)
 * - getRainRawValue()    -> Returns raw 0 or 1
 */

// --- Modül İçi Değişkenler ---
bool  _isRainingInternal = false;
int   _rainRawValueInternal = 1; // Başlangıçta Kuru (HIGH)

/**
 * Sensör pinini hazırlar.
 * RAIN_SENSOR_PIN değeri config.h dosyasından gelir.
 */
void rainSensorSetup() {
    pinMode(RAIN_SENSOR_PIN, INPUT);
    Serial.println("[RAIN] Modül GPIO " + String(RAIN_SENSOR_PIN) + " üzerinde hazır.");
}

/**
 * Sensörü okur ve durumu günceller.
 * Ana loop içerisinde her sensör okuma periyodunda çağrılmalıdır.
 */
void rainSensorUpdate() {
    // Sensörden dijital okuma yap (0 = Islak, 1 = Kuru)
    _rainRawValueInternal = digitalRead(RAIN_SENSOR_PIN);
    
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