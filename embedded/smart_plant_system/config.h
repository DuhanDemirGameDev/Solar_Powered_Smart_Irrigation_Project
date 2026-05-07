/*
 * config.h
 * --------
 * Shared Configuration File for the Smart Plant Watering System
 */

#ifndef CONFIG_H
#define CONFIG_H

// ============================================================
//  WiFi Configuration
// ============================================================
#define WIFI_SSID       "Duhan"
#define WIFI_PASSWORD   "duhannnn"

// ============================================================
//  Backend Server Configuration
// ============================================================
// IMPORTANT: Replace 192.168.1.X with the IP address of the machine
// running the Spring Boot backend on the same WiFi network as the ESP32.
#define BACKEND_HOST    "10.15.147.126"
#define BACKEND_PORT    8081
#define ENDPOINT_DATA   "/api/v1/sensors"
#define ENDPOINT_CMD    "/api/v1/irrigation/command"

// ============================================================
//  Pin Definitions
// ============================================================
#define MOISTURE_SENSOR_PIN       35
#define MOISTURE_POWER_PIN        4

#define RAIN_SENSOR_PIN           34
#define RAIN_POWER_PIN            14

#define MOSFET_PIN                25

// ============================================================
//  Smart Sleep Sensor Power Management
// ============================================================
#define SENSOR_WARMUP_MS          50
#define SENSOR_SETTLE_MS          10

// ============================================================
//  Moisture Sensor Calibration
// ============================================================
#define MOISTURE_AIR_VALUE        2700
#define MOISTURE_WATER_VALUE      250

// ============================================================
//  Pump / Watering Thresholds
// ============================================================
#define MOISTURE_THRESHOLD_LOW    40
#define MOISTURE_THRESHOLD_TARGET 65

// Durations are standardized to SECONDS across backend and ESP32.
#define PUMP_DURATION_CRITICAL    45
#define PUMP_DURATION_LOW         30
#define PUMP_DURATION_MODERATE    15
#define PUMP_DURATION_HEAT_SHORT  10

#define PUMP_MAX_RUNTIME          90

// ============================================================
//  Timing Intervals
// ============================================================
#define SENSOR_READ_INTERVAL_MS       5000
#define DATA_SEND_INTERVAL_MS         10000
#define COMMAND_CHECK_INTERVAL_MS     5000
#define AUTO_WATER_CHECK_INTERVAL_MS  10000

// ============================================================
//  MOSFET Configuration
// ============================================================
#define MOSFET_ACTIVE_HIGH true

#endif
