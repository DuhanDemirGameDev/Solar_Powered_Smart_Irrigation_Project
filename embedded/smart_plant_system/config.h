/*
 * config.h
 * --------
 * Shared Configuration File for the Smart Plant Watering System
 * 
 * This file is used by ALL modules (WiFi, relay, moisture, rain, etc.)
 * Each team member should add their sensor's pin definitions and
 * parameters to the appropriate section below.
 */

#ifndef CONFIG_H
#define CONFIG_H

// ============================================================
//  WiFi Configuration (Shared — used by wifi_communication.ino)
// ============================================================
#define WIFI_SSID       "YOUR_WIFI_SSID"
#define WIFI_PASSWORD   "YOUR_WIFI_PASSWORD"

// ============================================================
//  Backend Server Configuration (Shared)
// ============================================================
#define BACKEND_HOST    "192.168.1.100"         // Backend server IP address
#define BACKEND_PORT    8000                     // Backend server port
#define ENDPOINT_DATA   "/api/sensor-data"       // POST: send all sensor data
#define ENDPOINT_CMD    "/api/pump-command"       // GET:  receive pump commands

// ============================================================
//  Pin Definitions (from Hardware Pin Schema)
//  Each team member defines their sensor pins here.
// ============================================================

// --- Capacitive Soil Moisture Sensor (Responsible: Teammate) ---
#define MOISTURE_SENSOR_PIN   35    // AOUT -> GPIO 32

// --- Rain/Snow Sensor (Responsible: Team member) ---
#define RAIN_SENSOR_PIN       33    // D0 -> GPIO 25

// --- 1 Channel Relay Module / Water Pump (Responsible: [Your Name]) ---
#define RELAY_PIN             35    // IN -> GPIO 26

// ============================================================
//  Moisture Sensor Calibration
//  (Defined by moisture sensor teammate.
//   Kept here so it's accessible system-wide via config.h)
// ============================================================
// NOTE: The following values should be set by the moisture sensor
//       team member after calibrating their sensor.
#define MOISTURE_AIR_VALUE    3500    // ADC reading in air   = 0% moisture
#define MOISTURE_WATER_VALUE  1500    // ADC reading in water = 100% moisture

// ============================================================
//  Pump / Watering Thresholds
// ============================================================
#define MOISTURE_THRESHOLD_LOW    30    // Below this % → plant needs water
#define MOISTURE_THRESHOLD_TARGET 30    // Stop watering when reaching this %

// Watering durations based on moisture level (seconds)
#define PUMP_DURATION_CRITICAL    180   // 0-10%:   3 minutes
#define PUMP_DURATION_LOW         120   // 10-20%:  2 minutes
#define PUMP_DURATION_MODERATE    60    // 20-30%:  1 minute
#define PUMP_DURATION_HEAT_SHORT  30    // Hot weather short burst

// Safety: absolute max pump run time (seconds)
#define PUMP_MAX_RUNTIME          300   // 5 minutes max

// ============================================================
//  Timing Intervals (milliseconds)
// ============================================================
#define SENSOR_READ_INTERVAL_MS    5000    // Read sensors every 5 seconds
#define DATA_SEND_INTERVAL_MS      15000   // Send data to backend every 15 seconds
#define COMMAND_CHECK_INTERVAL_MS  10000   // Check for commands every 10 seconds

// ============================================================
//  Relay Logic
//  Set to true if your relay is ACTIVE LOW (most modules are)
// ============================================================
#define RELAY_ACTIVE_LOW  true

#endif // CONFIG_H
