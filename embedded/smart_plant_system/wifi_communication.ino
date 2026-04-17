/*
 * wifi_communication.ino
 * ======================
 * Unified WiFi Communication Module for the Entire Electronic Side
 * 
 * This file is SHARED across all team members. It handles:
 *   1. WiFi connection to the network (with auto-reconnect)
 *   2. Sending ALL sensor data to the backend via HTTP POST
 *   3. Receiving commands from the backend via HTTP GET
 *   4. JSON serialization/deserialization
 * 
 * Every sensor module (moisture, rain, etc.) provides getter functions.
 * This module collects all their data and sends it in ONE unified POST.
 * 
 * Backend API Contract:
 * 
 * POST /api/sensor-data
 * {
 *   "moisture_percent": 25.5,
 *   "moisture_raw": 2800,
 *   "is_raining": false,
 *   "rain_sensor_raw": 1,
 *   "pump_state": "IDLE",
 *   "pump_remaining_time": 0
 * }
 * 
 * GET /api/pump-command
 * Response:
 * {
 *   "action": "start" | "stop" | "heat_burst" | "none",
 *   "duration": 120,
 *   "reason": "LLM says: Moisture is critically low."
 * }
 * 
 * Required Libraries:
 *   - WiFi.h        (built-in with ESP32 board package)
 *   - HTTPClient.h  (built-in with ESP32 board package)
 *   - ArduinoJson   (install via Library Manager, v6.x)
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include "types.h"

// ============================================================
//  WiFi State
// ============================================================
bool   wifiConnected          = false;
unsigned long lastReconnectAttempt = 0;
int    reconnectAttempts      = 0;

#define RECONNECT_INTERVAL_MS    5000
#define MAX_RECONNECT_ATTEMPTS   20
#define HTTP_TIMEOUT_MS          5000

// ============================================================
//  WiFi Setup — Connect to Network
// ============================================================
void wifiSetup() {
    Serial.println("[WIFI] Connecting to: " + String(WIFI_SSID));

    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    // Wait for initial connection (max 20 seconds)
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 40) {
        delay(500);
        Serial.print(".");
        attempts++;
    }
    Serial.println();

    if (WiFi.status() == WL_CONNECTED) {
        wifiConnected = true;
        Serial.println("[WIFI] ✓ Connected!");
        Serial.println("[WIFI] IP: " + WiFi.localIP().toString());
        Serial.println("[WIFI] RSSI: " + String(WiFi.RSSI()) + " dBm");
    } else {
        wifiConnected = false;
        Serial.println("[WIFI] ✗ Connection failed. Will retry in background.");
    }
}

// ============================================================
//  WiFi Maintain — Auto-Reconnect (call in loop)
// ============================================================
void wifiMaintain() {
    if (WiFi.status() == WL_CONNECTED) {
        if (!wifiConnected) {
            wifiConnected = true;
            reconnectAttempts = 0;
            Serial.println("[WIFI] ✓ Reconnected! IP: " + WiFi.localIP().toString());
        }
        return;
    }

    // WiFi lost — attempt reconnection
    wifiConnected = false;

    if (millis() - lastReconnectAttempt < RECONNECT_INTERVAL_MS) {
        return;
    }

    lastReconnectAttempt = millis();
    reconnectAttempts++;

    if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
        Serial.println("[WIFI] Max reconnect attempts. Restarting WiFi...");
        WiFi.disconnect();
        delay(1000);
        WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
        reconnectAttempts = 0;
        return;
    }

    Serial.println("[WIFI] Reconnecting... attempt " + String(reconnectAttempts));
    WiFi.reconnect();
}

// ============================================================
//  Check WiFi Status
// ============================================================
bool isWifiConnected() {
    return wifiConnected && (WiFi.status() == WL_CONNECTED);
}

// ============================================================
//  Build Full URL from Endpoint
// ============================================================
String buildUrl(const char* endpoint) {
    return "http://" + String(BACKEND_HOST) + ":" + String(BACKEND_PORT) + String(endpoint);
}

// ============================================================
//  Send ALL Sensor Data to Backend (POST)
//  
//  This function collects data from every sensor module
//  and sends it as one unified JSON payload.
//  
//  ► Each team member's sensor module provides getter functions.
//  ► Add your sensor's data to the JSON below when integrating.
// ============================================================
bool sendAllSensorData(float moisturePercent, int moistureRaw,
                       bool isRaining, int rainRaw,
                       const char* pumpState, int pumpRemainingTime) {
    
    if (!isWifiConnected()) {
        Serial.println("[WIFI] Cannot send data: not connected.");
        return false;
    }

    HTTPClient http;
    String url = buildUrl(ENDPOINT_DATA);

    http.begin(url);
    http.setTimeout(HTTP_TIMEOUT_MS);
    http.addHeader("Content-Type", "application/json");

    // --- Build unified JSON payload with ALL sensor data ---
    StaticJsonDocument<512> doc;

    // Moisture sensor data (from moisture_sensor.ino)
    doc["moisture_percent"]    = round(moisturePercent * 10.0) / 10.0;
    doc["moisture_raw"]        = moistureRaw;

    // Rain sensor data (from rain_sensor.ino — your teammate's module)
    doc["is_raining"]          = isRaining;
    doc["rain_sensor_raw"]     = rainRaw;

    // Pump status (from relay_control.ino)
    doc["pump_state"]          = pumpState;
    doc["pump_remaining_time"] = pumpRemainingTime;

    // ──────────────────────────────────────────────────────
    // ► TEAM: Add more sensor data here as needed
    // Example:
    //   doc["temperature"]     = getTemperature();
    //   doc["solar_voltage"]   = getSolarVoltage();
    // ──────────────────────────────────────────────────────

    String payload;
    serializeJson(doc, payload);

    Serial.println("[WIFI] POST → " + url);
    Serial.println("[WIFI] Payload: " + payload);

    int httpCode = http.POST(payload);

    if (httpCode > 0) {
        String response = http.getString();
        Serial.println("[WIFI] Response (" + String(httpCode) + "): " + response);
        http.end();
        return (httpCode == 200 || httpCode == 201);
    } else {
        Serial.println("[WIFI] POST failed: " + http.errorToString(httpCode));
        http.end();
        return false;
    }
}

// ============================================================
//  Check Backend for Pump Commands (GET)
//  
//  Returns a BackendCommand struct with the action to perform.
//  The main sketch will route this command to relay_control.
// ============================================================
BackendCommand checkBackendForCommand() {
    BackendCommand cmd;
    cmd.hasCommand = false;
    cmd.action     = "none";
    cmd.duration   = 0;
    cmd.reason     = "";

    if (!isWifiConnected()) {
        return cmd;
    }

    HTTPClient http;
    String url = buildUrl(ENDPOINT_CMD);

    http.begin(url);
    http.setTimeout(HTTP_TIMEOUT_MS);

    int httpCode = http.GET();

    if (httpCode == 200) {
        String response = http.getString();

        // Parse JSON response
        StaticJsonDocument<512> doc;
        DeserializationError error = deserializeJson(doc, response);

        if (error) {
            Serial.println("[WIFI] JSON parse error: " + String(error.c_str()));
            http.end();
            return cmd;
        }

        cmd.action   = doc["action"].as<String>();
        cmd.duration = doc["duration"] | 0;
        cmd.reason   = doc["reason"] | "No reason provided";

        if (cmd.action != "none" && cmd.action.length() > 0) {
            cmd.hasCommand = true;
            Serial.println("[WIFI] ← Command: " + cmd.action
                + " | Duration: " + String(cmd.duration) + "s"
                + " | Reason: " + cmd.reason);
        }
    } else if (httpCode > 0) {
        Serial.println("[WIFI] GET response: " + String(httpCode));
    } else {
        Serial.println("[WIFI] GET failed: " + http.errorToString(httpCode));
    }

    http.end();
    return cmd;
}
