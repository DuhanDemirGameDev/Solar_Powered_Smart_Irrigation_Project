/*
 * config.h
 * --------
 * Shared Configuration File for the Smart Plant Watering System
 * 
 * This file is used by ALL modules (WiFi, MOSFET, moisture, rain, etc.)
 * Each team member should add their sensor's pin definitions and
 * parameters to the appropriate section below.
 *
 * HARDWARE CHANGE LOG:
 *   - Relay module REMOVED → replaced with N-Channel MOSFET on GPIO25
 *   - Sensor VCC pins NOT connected to 3.3V directly
 *   - Sensors use "Smart Sleep" power management:
 *       • Moisture sensor VCC → GPIO4  (powered on/off by software)
 *       • Rain sensor VCC    → GPIO14 (powered on/off by software)
 *     This saves power between readings — critical for solar operation.
 */

#ifndef CONFIG_H
#define CONFIG_H

// ============================================================
//  WiFi Configuration (Shared — used by wifi_communication.ino)
// ============================================================
#define WIFI_SSID       "realme 8"
#define WIFI_PASSWORD   "gdkuesy6"

// ============================================================
//  Backend Server Configuration (Shared)
// ============================================================
#define BACKEND_HOST    "10.188.181.203"         // Backend server IP address
#define BACKEND_PORT    8000                     // Backend server port
#define ENDPOINT_DATA   "/api/sensor-data"       // POST: send all sensor data
#define ENDPOINT_CMD    "/api/pump-command"       // GET:  receive pump commands

// ============================================================
//  Pin Definitions (Updated Hardware Pin Schema)
// ============================================================

// --- Capacitive Soil Moisture Sensor ---
//     AOUT → GPIO35 (ADC1_CH7, input-only pin — ideal for analog read)
//     VCC  → GPIO4  (Smart Sleep: software-controlled power)
#define MOISTURE_SENSOR_PIN       35
#define MOISTURE_POWER_PIN        4     // Powers the moisture sensor on/off

// --- Rain/Snow Sensor (e.g., YL-83 / MH-RD) ---
//     D0  → GPIO34 (ADC1_CH6, input-only pin — digital read)
//     VCC → GPIO14 (Smart Sleep: software-controlled power)
#define RAIN_SENSOR_PIN           34
#define RAIN_POWER_PIN            14    // Powers the rain sensor on/off

// --- N-Channel MOSFET → Water Pump ---
//     GATE → GPIO25 (Digital Output — drives the MOSFET gate)
//     DRAIN → Pump negative terminal
//     SOURCE → GND
#define MOSFET_PIN                25

// ============================================================
//  Smart Sleep — Sensor Power Management
//  Instead of connecting sensor VCC pins directly to 3.3V,
//  we route them through GPIO pins. This allows the ESP32 to
//  cut power to the sensors between readings, dramatically
//  reducing current draw — essential for solar-powered systems.
//
//  HOW IT WORKS:
//    1. Before reading, set the power pin HIGH → sensor gets 3.3V
//    2. Wait a short warm-up period for the sensor to stabilize
//    3. Take the reading
//    4. Set the power pin LOW → sensor is powered off (0V)
//
//  WHY GPIO4 and GPIO14?
//    • Both are general-purpose output-capable pins on ESP32
//    • GPIO4:  No special boot function, safe for output at startup
//    • GPIO14: Outputs PWM at boot but settles — safe for sensor power
//    • Each GPIO can source ~40mA max (per Espressif datasheet),
//      which is sufficient for typical sensor modules:
//        - Capacitive moisture sensor: ~5-10mA
//        - Rain sensor module: ~5-15mA
//    • If a sensor draws more than 40mA, a transistor/MOSFET
//      switch would be needed on the power line instead.
//
//  IMPORTANT: GPIO34 and GPIO35 are INPUT-ONLY on ESP32.
//  That is exactly why they are used as sensor DATA pins
//  (not power pins). They cannot output voltage.
// ============================================================
#define SENSOR_WARMUP_MS          50    // ms to wait after powering sensor ON
#define SENSOR_SETTLE_MS          10    // ms to wait after powering sensor OFF

// ============================================================
//  Moisture Sensor Calibration
// ============================================================
// CALIBRATED VALUES (measured from actual sensor):
//   Sensor in dry air / completely dry soil → ADC ≈ 2700 → 0% moisture
//   Sensor fully submerged in water         → ADC ≈ 250  → 100% moisture
//
// Mapping formula:  moisture% = ((AIR - raw) / (AIR - WATER)) * 100
//   raw=2700 → (2700-2700)/(2700-250)*100 =   0%
//   raw=250  → (2700-250) /(2700-250)*100 = 100%
#define MOISTURE_AIR_VALUE    2700    // ADC reading in dry air = 0% moisture
#define MOISTURE_WATER_VALUE  250     // ADC reading in water   = 100% moisture

// ============================================================
//  Pump / Watering Thresholds
// ============================================================
#define MOISTURE_THRESHOLD_LOW    40    // Below this % → plant needs water (most plants wilt below 40%)
#define MOISTURE_THRESHOLD_TARGET 65    // Stop watering when reaching this % (moist but not soggy)

// Watering durations based on moisture level (seconds)
// NOTE: Reduced from original (180/120/60/30) to protect the pump motor.
//       Shorter, more frequent bursts are safer than long continuous runs.
//       The real-time moisture auto-stop will cut the pump early if soil
//       reaches MOISTURE_THRESHOLD_TARGET before the timer expires.
#define PUMP_DURATION_CRITICAL    45    // 0-13%:   45 seconds (very dry soil)
#define PUMP_DURATION_LOW         30    // 13-26%:  30 seconds (dry soil)
#define PUMP_DURATION_MODERATE    15    // 26-40%:  15 seconds (slightly dry)
#define PUMP_DURATION_HEAT_SHORT  10    // Hot weather short burst

// Safety: absolute max pump run time (seconds)
#define PUMP_MAX_RUNTIME          90    // 1.5 minutes max (pump protection)

// ============================================================
//  Timing Intervals (milliseconds)
// ============================================================
#define SENSOR_READ_INTERVAL_MS    5000    // Read sensors every 5 seconds
#define DATA_SEND_INTERVAL_MS      10000   // Send data to backend every 10 seconds
#define COMMAND_CHECK_INTERVAL_MS  5000    // Check for commands every 5 seconds
#define AUTO_WATER_CHECK_INTERVAL_MS 10000 // Check auto-watering conditions every 10 seconds

// ============================================================
//  MOSFET Configuration
//  N-Channel MOSFET is ACTIVE HIGH:
//    HIGH on GATE → MOSFET conducts → Pump ON
//    LOW  on GATE → MOSFET blocks   → Pump OFF
//  (This is simpler than relay modules which are often ACTIVE LOW)
// ============================================================
#define MOSFET_ACTIVE_HIGH  true

#endif // CONFIG_H
