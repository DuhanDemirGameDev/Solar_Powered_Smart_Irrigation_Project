// moisture_sensor.ino
// Artık config.h dahil olduğu için tanımlamalara gerek yok

float currentMoisturePercent = 0.0;
int currentRawValue = 0;

void moistureSensorSetup() {
    analogReadResolution(12);
    // MOISTURE_SENSOR_PIN değeri config.h'den geliyor 
    pinMode(MOISTURE_SENSOR_PIN, INPUT); 
}

void moistureSensorUpdate() {
    currentRawValue = analogRead(MOISTURE_SENSOR_PIN);
    
    // map() içindeki değerler config.h'den çekiliyor
    currentMoisturePercent = map(currentRawValue, MOISTURE_AIR_VALUE, MOISTURE_WATER_VALUE, 0, 100);
    
    if (currentMoisturePercent < 0)   currentMoisturePercent = 0;
    if (currentMoisturePercent > 100) currentMoisturePercent = 100;
}

float getMoisturePercentage() { return currentMoisturePercent; }
int getMoistureRaw() { return currentRawValue; }