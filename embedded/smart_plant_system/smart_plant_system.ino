/*
 * smart_plant_system.ino
 * ======================
 * Main Sketch — Smart Plant Watering System
 * 
 * This is the entry point. It initializes all modules and runs
 * the main loop that coordinates sensor reading, data sending,
 * and command execution.
 * 
 * Architecture:
 *   ┌─────────────────────────────────────────────────┐
 *   │              smart_plant_system.ino              │
 *   │           (Main Sketch — this file)              │
 *   │                                                  │
 *   │  Calls setup/update functions from:              │
 *   │    ├── moisture_sensor.ino  (teammate's module)  │
 *   │    ├── relay_control.ino    (your module)        │
 *   │    ├── rain_sensor.ino      (teammate's module)  │
 *   │    └── wifi_communication.ino (shared module)    │
 *   └─────────────────────────────────────────────────┘
 *         ▲                           │
 *         │  GET /api/pump-command    │  POST /api/sensor-data
 *         │  (commands from LLM)      │  (all sensor values)
 *         │                           ▼
 *   ┌─────────────────────────────────────────────────┐
 *   │                 Backend Server                   │
 *   │         (API + LLM + Weather API)                │
 *   └─────────────────────────────────────────────────┘
 * 
 * Hardware (ESP32 DevKit V1):
 *   - Capacitive Moisture Sensor  → GPIO 32 (Analog)
 *   - Rain/Snow Sensor            → GPIO 25 (Digital)
 *   - 1-Channel Relay + Pump      → GPIO 26 (Digital)
 *   - Solar Panel (power)
 * 
 * Libraries Required:
 *   - ArduinoJson (v6.x) — Install via Arduino Library Manager
 *   - ESP32 Board Package — Provides WiFi.h, HTTPClient.h
 * 
 * Arduino IDE Setup:
 *   Board: "ESP32 Dev Module"
 *   Upload Speed: 115200
 *   All .ino files in this folder are compiled as one sketch.
 */

#include "config.h"
#include "types.h"

// ============================================================
//  Timing Trackers
// ============================================================
unsigned long lastSensorRead   = 0;
unsigned long lastDataSend     = 0;
unsigned long lastCommandCheck = 0;

// ============================================================
//  Rain Sensor State
//  (Will be managed by teammate's rain_sensor.ino module.
//   For now, read directly here. Replace with their functions
//   once rain_sensor.ino is ready.)
// ============================================================
bool isRaining    = false;
int  rainRawValue = 0;

// ============================================================
//  Moisture Sensor State
//  (Will be managed by teammate's moisture_sensor.ino module.
//   For now, read directly here. Replace with their functions
//   once moisture_sensor.ino is ready.)
// ============================================================
float moisturePercent = 0.0;
int   moistureRaw    = 0;

// ============================================================
//  SETUP
// ============================================================
void setup() {
    Serial.begin(115200);
    delay(1000);

    Serial.println();
    Serial.println("╔══════════════════════════════════════════╗");
    Serial.println("║    Smart Plant Watering System v1.0      ║");
    Serial.println("║    ESP32 — Electronic Control Unit       ║");
    Serial.println("╚══════════════════════════════════════════╝");
    Serial.println();

    // --- Rain Sensor (temporary — until teammate's module is ready) ---
    pinMode(RAIN_SENSOR_PIN, INPUT);
    Serial.println("[RAIN] Rain sensor initialized on GPIO " + String(RAIN_SENSOR_PIN));

    // --- Moisture Sensor ---
    // Eski geçici blokları sildik, senin fonksiyonunu çağırdık
    moistureSensorSetup(); 
    Serial.println("[MOISTURE] Modül hazır.");

    // --- Relay / Pump (your module) ---
    relaySetup();               // from relay_control.ino

    // --- WiFi Communication (shared module) ---
    wifiSetup();                // from wifi_communication.ino

    Serial.println();
    Serial.println("[SYSTEM] ✓ All modules initialized.");
    Serial.println("──────────────────────────────────────────");
}

// ============================================================
//  MAIN LOOP
// ============================================================
void loop() {
    unsigned long now = millis();

    // --- Always: Update pump state machine ---
    relayUpdate();              // from relay_control.ino

    // --- Always: Maintain WiFi connection ---
    wifiMaintain();             // from wifi_communication.ino

    // --- Periodic: Read all sensors ---
    if (now - lastSensorRead >= SENSOR_READ_INTERVAL_MS) {
        lastSensorRead = now;
        readAllSensors();
    }

    // --- Periodic: Send data to backend ---
    if (now - lastDataSend >= DATA_SEND_INTERVAL_MS) {
        lastDataSend = now;
        sendDataToBackend();
    }

    // --- Periodic: Check backend for commands ---
    if (now - lastCommandCheck >= COMMAND_CHECK_INTERVAL_MS) {
        lastCommandCheck = now;
        checkAndExecuteCommand();
    }

    delay(10);  // Prevent watchdog reset
}

// ============================================================
//  Read All Sensors
// ============================================================
void readAllSensors() {
    // 1. Kendi modülündeki okuma ve hesaplama işlemini çalıştır
    moistureSensorUpdate(); 
    
    // 2. Senin modülünde hesaplanan güncel değerleri çek
    moisturePercent = getMoisturePercentage();
    moistureRaw = getMoistureRaw();

    // 3. Yakup'un röle modülüne bu veriyi gönder
    updateMoistureData(moisturePercent); 

    // --- Rain Sensor (Geçici direkt okuma devam edebilir) ---
    rainRawValue = digitalRead(RAIN_SENSOR_PIN);
    isRaining = (rainRawValue == LOW);

    // --- Log Çıktısı ---
    Serial.println("[SENSORS] Moisture: " + String(moisturePercent, 1)
        + "% (raw: " + String(moistureRaw) + ")"
        + " | Rain: " + String(isRaining ? "YES" : "NO")
        + " | Pump: " + String(getPumpStateString()));
}

// ============================================================
//  Send Data to Backend (via WiFi module)
// ============================================================
void sendDataToBackend() {
    if (!isWifiConnected()) {
        Serial.println("[SYSTEM] Skipping data send — WiFi disconnected.");
        return;
    }

    // Collect data from all sensor modules and send via WiFi
    bool success = sendAllSensorData(
        moisturePercent,                 // moisture (temporary read)
        moistureRaw,                     // moisture (temporary read)
        isRaining,                       // rain sensor (temporary)
        rainRawValue,                    // rain sensor (temporary)
        getPumpStateString(),            // from relay_control.ino
        getPumpRemainingTime()           // from relay_control.ino
    );

    if (success) {
        Serial.println("[SYSTEM] ✓ Data sent successfully.");
    } else {
        Serial.println("[SYSTEM] ✗ Data send failed.");
    }
}

// ============================================================
//  Check and Execute Backend Commands
// ============================================================
void checkAndExecuteCommand() {
    if (!isWifiConnected()) return;

    // Get command from backend (via WiFi module)
    BackendCommand cmd = checkBackendForCommand();  // from wifi_communication.ino

    if (!cmd.hasCommand) return;

    Serial.println("[SYSTEM] Executing: " + cmd.action + " | Reason: " + cmd.reason);

    // --- Route command to relay_control.ino ---
    if (cmd.action == "start") {
        if (cmd.duration > 0) {
            startPump(cmd.duration);        // Specific duration from backend
        } else {
            startSmartWatering();           // Auto duration from moisture level
        }
    }
    else if (cmd.action == "stop") {
        stopPump("backend_command");
    }
    else if (cmd.action == "heat_burst") {
        startHeatProtectionBurst();
    }
    else {
        Serial.println("[SYSTEM] Unknown command: " + cmd.action);
    }
}
