# Solar-Powered Smart Irrigation System

**Gazi University - Computer Engineering Department** **Microprocessors Lecture (Laboratory 1 - Group 4)** * **Project Leader:** Yakup KARATAŞ  
* **Embedded System & Firmware:** Sude Nur ÖZMEN, Zeynep Sude KİLECİOĞLU  
* **Backend, Web & Mobile Architect:** Duhan DEMİR  
* **AI & Weather Integration:** Muhammed Emin YOZĞAT  
* **Device Control & Alerts:** Hasan GÜRSES  
---

> An enterprise-style IoT irrigation platform combining renewable-energy hardware, ESP32 firmware, Spring Boot services, PostgreSQL persistence, a Python/Flask AI decision engine, a responsive web dashboard, and a native Android control application.

## 1. Project Overview & Architecture

The **Solar-Powered Smart Irrigation System** is a full-stack engineering project for autonomous, data-driven plant watering. It is designed to solve a real agricultural automation problem: water only when conditions justify it, preserve historical evidence, and allow a human operator to monitor or override the system from modern user interfaces.

The project is organized as a monorepo:

```text
Solar_Powered_Smart_Irrigation_Project/
|-- embedded/         ESP32 firmware, sensor logic, pump control, Wi-Fi config
|-- backend/          Spring Boot REST API, PostgreSQL integration, command queue
|-- ai-engine/        Python Flask inference service and trained ML model
|-- frontend-web/     Browser dashboard built with HTML, JavaScript, Bootstrap, Chart.js
|-- frontend-mobile/  Native Android application built with Java, Retrofit, Material UI
|-- docs/             Supporting project artifacts
`-- README.md         Single source of truth for setup, operation, and testing
```

### Core Pillar 1: ESP32 Hardware

The embedded layer is built around an **ESP32 microcontroller** and field-oriented irrigation electronics. It reads a capacitive soil moisture sensor, checks rain/snow conditions, sends sensor telemetry to the backend, polls the backend for irrigation commands, and controls the pump through the configured MOSFET/relay output.

The physical system is designed for solar-assisted operation:

- **Controller:** ESP32 development board.
- **Energy subsystem:** 6V solar panel, TP5100 charging module, 2S 18650 Li-ion battery pack, and LM2596 voltage regulator.
- **Sensors:** capacitive soil moisture sensor and rain/snow detection module.
- **Actuator:** DC water pump controlled through the firmware output stage.
- **Firmware architecture:** non-blocking `millis()`-based scheduling for sensor acquisition, network communication, command polling, and pump timing.

The firmware files live in:

```text
embedded/smart_plant_system/
```

Key configuration is centralized in:

```text
embedded/smart_plant_system/config.h
```

### Core Pillar 2: Spring Boot Backend

The backend is the coordination center of the system. It runs on **port `8081`** and exposes REST APIs for:

- ESP32 sensor ingestion.
- Sensor history retrieval.
- AI-assisted irrigation decision handling.
- Manual pump command queueing.
- ESP32 command polling.
- Irrigation event logging.
- Irrigation history retrieval.
- API documentation through Swagger UI.

It uses:

- **Java / Spring Boot**
- **Spring Web**
- **Spring Data JPA**
- **PostgreSQL**
- **Spring validation**
- **Springdoc OpenAPI**
- **JavaMailSender** for notification support

The database connection is configured in:

```text
backend/src/main/resources/application.properties
```

Important local backend values:

```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5436/smart_irrigation_db
spring.datasource.username=postgres
spring.datasource.password=1234
springdoc.swagger-ui.path=/swagger-ui.html
```

### Core Pillar 3: Python/Flask AI Engine

The AI engine is a lightweight Flask microservice running on **port `5000`**. It accepts sensor conditions, enriches them with weather context where available, and uses a trained Scikit-learn model to return an irrigation decision.

Its prediction endpoint is:

```text
POST http://127.0.0.1:5000/predict
```

The model is loaded from:

```text
ai-engine/model/irrigation_model.pkl
```

The service accepts a JSON payload such as:

```json
{
  "moisture": 20,
  "is_raining": false
}
```

Typical response:

```json
{
  "decision": "IRRIGATE",
  "moisture": 20.0,
  "rain_prob": 0,
  "temperature": 20,
  "humidity": 50,
  "is_raining": false
}
```

If the weather API or internet connection is unavailable, the AI service falls back to default weather values so local testing can continue.

### Core Pillar 4: Web Dashboard

The browser dashboard provides a responsive operational view for demonstrations, local testing, and desktop monitoring. It is served from:

```text
frontend-web/
```

When launched with the included static server, it runs on:

```text
http://localhost:5500
```

The web dashboard displays:

- Backend connectivity status.
- Latest sensor reading.
- Moisture history visualization.
- Rain and pump status.
- Recent irrigation logs.
- Manual pump controls where supported by the UI.

### Core Pillar 5: Native Android App

The Android application is a native Java app located in:

```text
frontend-mobile/
```

It uses:

- **Android Studio**
- **Java**
- **Retrofit2**
- **Gson converter**
- **Material Design components**
- **RecyclerView**
- **Bottom Navigation**

The app is organized around three primary screens:

- **Dashboard:** live moisture, rain, pump, and status information.
- **Control:** manual pump start and stop commands.
- **History:** paginated irrigation history.

The mobile app talks to the Spring Boot backend through Retrofit. The most important local networking file is:

```text
frontend-mobile/app/src/main/java/com/example/smartirrigationmobile/network/RetrofitClient.java
```

For the Android Emulator, the backend URL must be:

```java
private static final String BASE_URL = "http://10.0.2.2:8081/";
```

For a physical Android device, replace `10.0.2.2` with the IPv4 address of the computer running the backend:

```java
private static final String BASE_URL = "http://192.168.1.X:8081/";
```

`localhost` does not work from the emulator or physical phone in the same way it works from the host computer. On the emulator, `10.0.2.2` is the special alias for the host machine. On a physical phone, the phone and backend computer must be connected to the same local network.

### End-to-End Architecture Flow

```text
ESP32 sensors
  -> Spring Boot REST API on port 8081
  -> PostgreSQL persistence on localhost:5436
  -> Flask AI engine on port 5000
  -> Backend irrigation command queue
  -> ESP32 command polling
  -> Pump activation or stop
  -> Web dashboard and Android app visibility
```

This architecture supports both **autonomous irrigation** and **manual override** while keeping persistent records for testing, grading, troubleshooting, and demonstration.

## 2. Prerequisites (Required Software)

Install the following software before running the complete system:

| Requirement | Purpose | Notes |
| --- | --- | --- |
| **Java 17+** | Spring Boot backend | JDK 21 is recommended because the current Maven configuration targets Java 21. |
| **Python 3.x** | Flask AI engine | Python 3.10+ is recommended. |
| **Android Studio** | Native Android app | Required for Gradle sync, emulator testing, and APK deployment. |
| **Docker Desktop** | PostgreSQL and pgAdmin | Used by `backend/docker-compose.yml`. |
| **Arduino IDE or PlatformIO** | ESP32 firmware upload | Required to configure, compile, and flash the embedded system. |
| **Node.js 18+** | Web dashboard static server | Used by `frontend-web/serve-static.js`. |
| **Git** | Repository management | Recommended for cloning and version control. |
| **PowerShell or terminal** | Command execution | PowerShell commands are shown because this project is being run on Windows. |

Verify the main tools:

```powershell
docker --version
docker compose version
java -version
node --version
npm --version
py --version
```

If `py --version` is not available, use:

```powershell
python --version
```

Set a convenient project root variable from the repository root:

```powershell
cd "C:\path\to\Solar_Powered_Smart_Irrigation_Project"
$PROJECT_ROOT = (Get-Location).Path
```

If your terminal is already inside the repository root:

```powershell
$PROJECT_ROOT = (Get-Location).Path
```

## 3. Comprehensive Local Setup & Execution Guide (Step-by-Step)

Start each long-running service in a separate terminal. Recommended startup order:

1. Database
2. AI engine
3. Backend
4. Web dashboard
5. Android app
6. ESP32 firmware

### Step 1: Database - Start PostgreSQL with Docker Compose

Open **Terminal 1**:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "backend")
docker compose up -d
docker ps --filter name=smart_irrigation_db
```

Expected container mapping:

```text
smart_irrigation_db ... 0.0.0.0:5436->5432/tcp
```

Verify PostgreSQL login:

```powershell
docker exec smart_irrigation_db psql -U postgres -d smart_irrigation_db -c "SELECT current_database(), current_user;"
```

Expected result:

```text
smart_irrigation_db | postgres
```

Local database configuration:

```text
Database: smart_irrigation_db
User:     postgres
Password: 1234
Port:     5436 on the host machine, mapped to 5432 inside Docker
```

Optional pgAdmin is also started by Docker Compose:

```text
URL:      http://localhost:5050
Email:    admin@admin.com
Password: admin
```

### Step 2: Backend - Build and Run the Spring Boot Server on Port 8081

Open **Terminal 2**:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "backend")
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Expected startup indicators:

```text
Started BackendApplication
Tomcat started on port 8081
```

Backend URLs:

```text
API base:   http://localhost:8081
Swagger UI: http://localhost:8081/swagger-ui.html
```

Quick backend check from another terminal:

```powershell
Invoke-WebRequest -UseBasicParsing "http://localhost:8081/swagger-ui.html" |
  Select-Object StatusCode, StatusDescription
```

Expected:

```text
StatusCode: 200
```

### Step 3: AI Engine - Install Dependencies and Run Flask on Port 5000

Open **Terminal 3**:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "ai-engine")
```

Create a Python virtual environment:

```powershell
py -m venv .venv
```

If `py` is unavailable:

```powershell
python -m venv .venv
```

Activate the virtual environment:

```powershell
.\.venv\Scripts\Activate.ps1
```

If PowerShell blocks activation:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
```

Install the required AI dependencies:

```powershell
python -m pip install --upgrade pip
pip install flask flask-cors pandas scikit-learn
```

The repository also includes `ai-engine/requirements.txt`; you may install from it as an equivalent path:

```powershell
pip install -r requirements.txt
```

Start Flask:

```powershell
python .\service\app.py
```

Expected:

```text
Running on http://127.0.0.1:5000
```

Direct AI health test:

```powershell
Invoke-RestMethod `
  -Uri "http://127.0.0.1:5000/predict" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"moisture":20,"is_raining":false}' |
  ConvertTo-Json -Depth 10
```

Expected response contains a decision such as:

```json
{
  "decision": "IRRIGATE"
}
```

The exact decision can vary depending on the trained model and weather fallback/API result.

### Step 4: Web Dashboard - Serve the HTML/JS Frontend

Open **Terminal 4**:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "frontend-web")
node .\serve-static.js
```

Expected:

```text
Frontend dashboard running at http://localhost:5500
```

Open the dashboard in a browser:

```text
http://localhost:5500
```

The dashboard communicates with backend endpoints such as:

```text
http://localhost:8081/api/v1/sensors/history
http://localhost:8081/api/v1/irrigation/history
http://localhost:8081/api/v1/irrigation/set-command
```

If the dashboard shows a connection error:

- Confirm the backend is running on `http://localhost:8081`.
- Confirm the dashboard is served from `http://localhost:5500`.
- Confirm CORS includes port `5500` in `backend/src/main/resources/application.properties`.

### Step 5: Native Android App - Open, Configure, Sync, and Run


Open **Android Studio**, then select:

```text
frontend-mobile/
```

Wait for:

- Gradle project import.
- Gradle sync completion.
- Android Studio indexing.
- Dependency resolution.

Then run the app on either an Android Emulator or a physical Android device.

#### Critical Retrofit BASE_URL Configuration

Open:

```text
frontend-mobile/app/src/main/java/com/example/smartirrigationmobile/network/RetrofitClient.java
```

For the **Android Emulator**, use:

```java
private static final String BASE_URL = "http://10.0.2.2:8081/";
```

Why this matters:

- `localhost` inside the emulator means the emulator itself.
- `10.0.2.2` is Android Emulator's special address for the host computer.
- The Spring Boot backend must be running on the host computer at port `8081`.

For a **physical Android device**, use the host computer's LAN IPv4 address:

```java
private static final String BASE_URL = "http://192.168.1.X:8081/";
```

Physical device requirements:

- The phone and backend computer must be on the same Wi-Fi/network.
- Windows Firewall must allow inbound connections to port `8081`.
- The backend must bind normally through Spring Boot on port `8081`.

Find the computer's IPv4 address on Windows:

```powershell
ipconfig
```

Use the IPv4 address for the Wi-Fi adapter connected to the same network as the Android device.

> **⚠️ CRITICAL TROUBLESHOOTING FOR WINDOWS USERS:**
- If your project path contains non-ASCII or Turkish characters (e.g., `ı, ş, ü, ö, ç`), Android Studio Gradle Sync will fail. 
- **Fix:** Open `frontend-mobile/gradle.properties` and add this line at the bottom:
- `android.overridePathCheck=true`
- Then click "Sync Now".

### Step 6: Embedded System - Configure Wi-Fi/IP and Flash the ESP32

Open the embedded firmware folder in **Arduino IDE** or **PlatformIO**:

```text
embedded/smart_plant_system/
```

Edit:

```text
embedded/smart_plant_system/config.h
```

Configure Wi-Fi credentials:

```cpp
#define WIFI_SSID       "YOUR_WIFI_NAME"
#define WIFI_PASSWORD   "YOUR_WIFI_PASSWORD"
```

Configure backend host and port:

```cpp
#define BACKEND_HOST    "192.168.1.X"
#define BACKEND_PORT    8081
```

Important ESP32 networking rule:

- Do not use `localhost` for the ESP32 backend host.
- `localhost` on the ESP32 means the ESP32 itself.
- Use the LAN IPv4 address of the computer running Spring Boot.
- The ESP32 and backend computer must be connected to the same network.

Backend endpoints used by the firmware:

```cpp
#define ENDPOINT_DATA   "/api/v1/sensors"
#define ENDPOINT_CMD    "/api/v1/irrigation/command"
```

Recommended Arduino IDE board setup:

- Install ESP32 board support through Boards Manager.
- Select the correct ESP32 board model.
- Select the correct COM port.
- Compile and upload `smart_plant_system.ino`.
- Open Serial Monitor to inspect Wi-Fi connection, sensor readings, backend POSTs, command polls, and pump state changes.

Pump durations are standardized to **seconds** across Spring Boot, Flask-triggered decisions, frontend manual override, and ESP32 firmware.

## 4. System Flow & End-to-End Testing

### Operational Flow

The complete system works as follows:

```text
1. ESP32 reads soil moisture and rain sensor values.
2. ESP32 sends telemetry to Spring Boot: POST /api/v1/sensors.
3. Spring Boot persists the reading in PostgreSQL.
4. Spring Boot forwards relevant sensor context to Flask AI: POST /predict.
5. Flask returns an irrigation decision such as IRRIGATE or WAIT.
6. Spring Boot queues a pump command when irrigation is required.
7. ESP32 polls Spring Boot: GET /api/v1/irrigation/command.
8. ESP32 starts or stops the pump based on the returned command.
9. ESP32/backend logs pump activity for history.
10. Web dashboard and Android app display live state and history.
```

### Test 1: Send Mock Sensor Data to the Backend

Open a new terminal after all services are running:

```powershell
$BASE = "http://localhost:8081"
```

Send low moisture and no rain, simulating the ESP32:

```powershell
$sensorBody = @{
  moisture_percent = 20
  moisture_raw = 820
  is_raining = $false
  rain_sensor_raw = 950
  pump_state = "stop"
  pump_remaining_time = 0
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "$BASE/api/v1/sensors" `
  -Method Post `
  -ContentType "application/json" `
  -Body $sensorBody |
  ConvertTo-Json -Depth 10
```

Expected:

- The request succeeds.
- The response contains the saved sensor reading.
- The response includes an `id`.
- Backend logs show a database insert.
- Flask logs show a `/predict` request.

### Test 2: Verify the AI Decision Changes System State

Check the Flask terminal. You should see:

```text
POST /predict HTTP/1.1 200
```

Poll the backend command endpoint:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

If AI returned `IRRIGATE`, expect a queued start command:

```json
{
  "duration": 15,
  "unit": "seconds",
  "reason": "AI decision: IRRIGATE",
  "hasCommand": true,
  "action": "start"
}
```

If AI returned `WAIT`, expect no pump command:

```json
{
  "duration": 0,
  "unit": "seconds",
  "reason": "AI decision: WAIT",
  "hasCommand": false,
  "action": "none"
}
```

If Flask is offline, the backend should remain stable and report that no AI command is available:

```json
{
  "duration": 0,
  "unit": "seconds",
  "reason": "AI decision: AI_SERVICE_UNAVAILABLE",
  "hasCommand": false,
  "action": "none"
}
```

### Test 3: Verify Consume-on-Read Command Polling

If the first command poll returned `start`, poll again immediately:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected second poll:

```json
{
  "duration": 0,
  "unit": "seconds",
  "reason": "AI decision: IDLE",
  "hasCommand": false,
  "action": "none"
}
```

This confirms the ESP32 receives a queued command once instead of repeatedly starting the pump forever.

### Test 4: Use Manual Start Pump Command

Queue a manual pump start command. The duration value is in **seconds**:

```powershell
Invoke-RestMethod `
  -Uri "$BASE/api/v1/irrigation/set-command" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"action":"start","duration":5,"reason":"Manual E2E test"}' |
  ConvertTo-Json -Depth 10
```

Expected response:

```json
{
  "queued": true,
  "duration": 5,
  "unit": "seconds",
  "reason": "Manual E2E test",
  "action": "start"
}
```

The next ESP32 command poll should return:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected:

```json
{
  "duration": 5,
  "unit": "seconds",
  "reason": "Manual E2E test",
  "hasCommand": true,
  "action": "start"
}
```

Poll once more to confirm command consumption:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected:

```json
{
  "duration": 0,
  "unit": "seconds",
  "hasCommand": false,
  "action": "none"
}
```

### Test 5: Use Manual Stop Pump Command

Queue a manual stop command:

```powershell
Invoke-RestMethod `
  -Uri "$BASE/api/v1/irrigation/set-command" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"action":"stop","duration":0,"reason":"Manual E2E stop test"}' |
  ConvertTo-Json -Depth 10
```

Poll the ESP32 command endpoint:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected:

```json
{
  "duration": 0,
  "unit": "seconds",
  "reason": "Manual E2E stop test",
  "hasCommand": true,
  "action": "stop"
}
```

### Test 6: Verify Pump Logging and History

Simulate the ESP32 reporting that the pump started:

```powershell
Invoke-RestMethod `
  -Uri "$BASE/api/v1/irrigation/log" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"pumpStatus":"ON","durationInMinutes":5}' |
  ConvertTo-Json -Depth 10
```

Important compatibility note:

- `pumpStatus` must be exactly `ON` or `OFF`.
- Lowercase values such as `on`, `off`, `start`, or `stop` are invalid.
- The JSON field is still named `durationInMinutes` for API compatibility, but the current value represents pump duration in **seconds**.

Verify irrigation history:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/history?page=0&size=10" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected:

- `content` contains the new irrigation log.
- `pumpStatus` is `ON`.
- `durationInMinutes` contains the reported duration value.

### Test 7: Verify PostgreSQL Tables

Check tables:

```powershell
docker exec smart_irrigation_db psql -U postgres -d smart_irrigation_db -c "\dt"
```

Expected tables:

```text
irrigation_log
notification_log
sensor_data
```

Check saved sensor rows:

```powershell
docker exec smart_irrigation_db psql -U postgres -d smart_irrigation_db -c "SELECT id, moisture_percent, is_raining, pump_state, timestamp FROM sensor_data ORDER BY id DESC LIMIT 5;"
```

Check irrigation rows:

```powershell
docker exec smart_irrigation_db psql -U postgres -d smart_irrigation_db -c "SELECT id, pump_status, duration_in_minutes, timestamp FROM irrigation_log ORDER BY id DESC LIMIT 5;"
```

### Test 8: Verify Web Dashboard

Open:

```text
http://localhost:5500
```

You should see:

- Backend API card showing `http://localhost:8081`.
- Connection badge changing to a live/connected state.
- Latest Status showing the newest moisture reading.
- Moisture History chart displaying recent values.
- Recent Irrigation Logs showing pump activity after `/api/v1/irrigation/log`.

### Test 9: Verify Native Android Manual Pump Control

With the backend running on `8081` and `RetrofitClient.java` configured correctly:

1. Run the Android app from Android Studio.
2. Open the **Control** tab.
3. Tap the manual **Start Pump** control.
4. Watch the Spring Boot logs for a request to `/api/v1/irrigation/set-command`.
5. Poll `/api/v1/irrigation/command` or watch the ESP32 Serial Monitor to confirm the command is received.
6. Tap the manual stop control to confirm stop commands are queued and consumed.

This validates the mobile-to-backend-to-ESP32 control path.

### One-Shot E2E Smoke Test Script

After PostgreSQL, Flask, and Spring Boot are running, paste this into PowerShell:

```powershell
$BASE = "http://localhost:8081"

Write-Host "`n[1] Backend check"
Invoke-WebRequest -UseBasicParsing "$BASE/swagger-ui.html" | Select-Object StatusCode

Write-Host "`n[2] Flask AI direct check"
Invoke-RestMethod `
  -Uri "http://127.0.0.1:5000/predict" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"moisture":20,"is_raining":false}' |
  ConvertTo-Json -Depth 10

Write-Host "`n[3] Send sensor data to backend"
$sensorBody = @{
  moisture_percent = 20
  moisture_raw = 820
  is_raining = $false
  rain_sensor_raw = 950
  pump_state = "stop"
  pump_remaining_time = 0
} | ConvertTo-Json
Invoke-RestMethod -Uri "$BASE/api/v1/sensors" -Method Post -ContentType "application/json" -Body $sensorBody |
  ConvertTo-Json -Depth 10

Write-Host "`n[4] Poll command first time"
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10

Write-Host "`n[5] Poll command second time"
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10

Write-Host "`n[6] Queue manual start in seconds"
Invoke-RestMethod `
  -Uri "$BASE/api/v1/irrigation/set-command" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"action":"start","duration":5,"reason":"Smoke test manual start"}' |
  ConvertTo-Json -Depth 10

Write-Host "`n[7] Manual start first poll"
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10

Write-Host "`n[8] Manual start second poll"
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10

Write-Host "`n[9] Log pump ON action"
Invoke-RestMethod `
  -Uri "$BASE/api/v1/irrigation/log" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"pumpStatus":"ON","durationInMinutes":5}' |
  ConvertTo-Json -Depth 10

Write-Host "`n[10] Read histories"
Invoke-RestMethod -Uri "$BASE/api/v1/sensors/history?page=0&size=5" -Method Get |
  ConvertTo-Json -Depth 10
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/history?page=0&size=5" -Method Get |
  ConvertTo-Json -Depth 10
```

### Shutdown

Stop Spring Boot, Flask, and the frontend server with `Ctrl+C` in their terminals.

Stop Docker services:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "backend")
docker compose down
```

Reset all local database data only when you intentionally want a clean database:

```powershell
docker compose down -v
```

### Final Verification Checklist

Before presenting, grading, or committing:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "backend")
git diff --check
.\mvnw.cmd test
```

Expected:

- No merge conflict markers are printed.
- Maven ends with `BUILD SUCCESS`.
- PostgreSQL is reachable on `localhost:5436`.
- Flask responds on `http://127.0.0.1:5000/predict`.
- Spring Boot responds on `http://localhost:8081/swagger-ui.html`.
- Web dashboard loads at `http://localhost:5500`.
- Android app uses the correct `BASE_URL`.
- ESP32 `config.h` points to the backend computer's LAN IPv4 address.

### Academic and Engineering Significance

This project demonstrates a complete IoT product architecture rather than a single isolated prototype. It combines:

- Renewable-energy hardware design.
- Embedded sensor acquisition and actuator control.
- REST API design and persistence.
- Machine-learning assisted decision-making.
- Browser-based operations monitoring.
- Native mobile control.
- Local end-to-end verification.

For a professor, this repository shows the complete path from physical sensor data to automated irrigation and human override. For a developer, this README is the single operational manual needed to install, run, debug, and validate the system from scratch.
