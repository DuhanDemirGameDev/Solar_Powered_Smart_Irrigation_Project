/*
 * smart_plant_system.ino
 * ======================
 * Main Sketch — Smart Plant Watering System (Final Modular Version)
 * * Bu dosya sistemin giriş noktasıdır. Tüm modülleri (Nem, Yağmur, Röle, WiFi)
 * koordine eder ve ana döngüyü yönetir.
 */

#include "config.h"
#include "types.h"

// ============================================================
//  Timing Trackers
// ============================================================
unsigned long lastSensorRead      = 0;
unsigned long lastDataSend        = 0;
unsigned long lastCommandCheck    = 0;
unsigned long lastAutoWaterCheck  = 0;

// ============================================================
//  Global Sensor States
// ============================================================
bool  isRaining      = false;
int   rainRawValue   = 0;
float moisturePercent = 0.0;
int   moistureRaw     = 0;

// ============================================================
//  SETUP
// ============================================================
void setup() {
    Serial.begin(115200);
    delay(1000);

    Serial.println();
    Serial.println("╔══════════════════════════════════════════╗");
    Serial.println("║    Smart Plant Watering System v1.1      ║");
    Serial.println("║    ESP32 — Electronic Control Unit       ║");
    Serial.println("╚══════════════════════════════════════════╝");
    Serial.println();

    // --- 1. Rain Sensor Module (Senin Modülün) ---
    rainSensorSetup(); 

    // --- 2. Moisture Sensor Module (Takım Arkadaşının Modülü) ---
    moistureSensorSetup(); 

    // --- 3. Relay / Pump Module (Yakup'un Modülü) ---
    relaySetup();               

    // --- 4. WiFi Communication Module (Ortak Modül) ---
    wifiSetup();                

    Serial.println();
    Serial.println("[SYSTEM] ✓ Tüm modüller başarıyla bağlandı.");
    Serial.println("──────────────────────────────────────────");
}

// ============================================================
//  MAIN LOOP
// ============================================================
void loop() {
    unsigned long now = millis();

    // Pompa durum makinesini ve WiFi bağlantısını her döngüde kontrol et
    relayUpdate();              
    wifiMaintain();             

    // Periyodik: Sensörleri oku (Örn: 5 saniyede bir)
    if (now - lastSensorRead >= SENSOR_READ_INTERVAL_MS) {
        lastSensorRead = now;
        readAllSensors();
    }

    // Periyodik: Verileri backend'e gönder (Örn: 15 saniyede bir)
    if (now - lastDataSend >= DATA_SEND_INTERVAL_MS) {
        lastDataSend = now;
        sendDataToBackend();
    }

    // Periyodik: Backend'den gelen komutları kontrol et (Örn: 10 saniyede bir)
    if (now - lastCommandCheck >= COMMAND_CHECK_INTERVAL_MS) {
        lastCommandCheck = now;
        checkAndExecuteCommand();
    }

    // Periyodik: Otomatik sulama koşullarını kontrol et (Örn: 60 saniyede bir)
    if (now - lastAutoWaterCheck >= AUTO_WATER_CHECK_INTERVAL_MS) {
        lastAutoWaterCheck = now;
        autoWateringCheck();
    }

    delay(10);  // İşlemciyi rahatlatmak için kısa bekleme
}

// ============================================================
//  Read All Sensors (Modül Entegrasyon Noktası)
// ============================================================
void readAllSensors() {
    // --- 1. Moisture Sensor İşlemleri ---
    moistureSensorUpdate(); 
    moisturePercent = getMoisturePercentage();
    moistureRaw = getMoistureRaw();

    // Nem verisini karar vermesi için Yakup'un röle modülüne aktar
    updateMoistureData(moisturePercent); 

    // --- 2. Rain Sensor İşlemleri (Senin Yeni Modülün) ---
    rainSensorUpdate();             // Fiziksel okumayı yap
    isRaining = getIsRaining();     // Modülden mantıksal durumu al
    rainRawValue = getRainRawValue(); // Modülden ham değeri al

    // Yağmur verisini röle modülüne aktar (otomatik sulama kararı için)
    updateRainData(isRaining);

    // --- Log Çıktısı ---
    Serial.println("[SENSORS] Nem: %" + String(moisturePercent, 1)
        + " (raw: " + String(moistureRaw) + ")"
        + " | Yağmur: " + String(isRaining ? "EVET" : "HAYIR")
        + " | Pompa: " + String(getPumpStateString()));
}

// ============================================================
//  Send Data to Backend
// ============================================================
void sendDataToBackend() {
    if (!isWifiConnected()) return;

    bool success = sendAllSensorData(
        moisturePercent,
        moistureRaw,
        isRaining,
        rainRawValue,
        getPumpStateString(),
        getPumpRemainingTime()
    );

    if (success) Serial.println("[SYSTEM] ✓ Veri başarıyla gönderildi.");
}

// ============================================================
//  Check and Execute Backend Commands
// ============================================================
void checkAndExecuteCommand() {
    if (!isWifiConnected()) return;

    BackendCommand cmd = checkBackendForCommand();

    if (!cmd.hasCommand) return;

    Serial.println("[SYSTEM] Komut Alındı: " + cmd.action + " | Neden: " + cmd.reason);

    if (cmd.action == "start") {
        if (cmd.duration > 0) startPumpManual(cmd.duration);  // Manual: skip moisture auto-stop
        else startSmartWatering();
    }
    else if (cmd.action == "stop") {
        stopPump("backend_command");
    }
    else if (cmd.action == "heat_burst") {
        startHeatProtectionBurst();
    }
}

// ============================================================
//  Automatic Watering Check
//  Called periodically. Evaluates rain + moisture conditions
//  and starts watering autonomously when needed.
// ============================================================
void autoWateringCheck() {
    // Pompa zaten çalışıyorsa veya bekleme süresindeyse atla
    if (isPumpRunning() || isPumpInCooldown()) return;

    Serial.println("[SYSTEM] Otomatik sulama kontrolü..."
        " | Yağmur: " + String(isRaining ? "EVET" : "HAYIR")
        + " | Nem: %" + String(moisturePercent, 1));

    // startSmartWatering() zaten yağmur ve nem kontrolü yapıyor
    startSmartWatering();
}