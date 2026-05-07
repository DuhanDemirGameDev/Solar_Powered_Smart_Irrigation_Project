# Smart Irrigation Project - Local Setup & E2E Testing README

This guide starts the complete local system and verifies the full data flow:

ESP32-style sensor request -> Spring Boot backend -> Flask AI service -> PostgreSQL -> ESP32 command polling -> Web dashboard.

## Part 1: Prerequisites & Installation

Install these tools before starting.

### Required Tools

- Docker Desktop
- Java JDK 21
- Maven, or use the included Maven wrapper `mvnw.cmd`
- Python 3.10 or newer
- Node.js 18 or newer
- PowerShell
- Git

### Recommended Checks

Run these commands from PowerShell:

```powershell
docker --version
docker compose version
java -version
node --version
npm --version
py --version
```

If `py --version` does not work, try:

```powershell
python --version
```

### Project Paths

Open your terminal in the root folder of your cloned repository, then set `$PROJECT_ROOT` from the current location:

```powershell
cd "C:\path\to\your\cloned\Solar_Powered_Smart_Irrigation_Project"
$PROJECT_ROOT = (Get-Location).Path
```

If you are already inside the cloned repository root, just run:

```powershell
$PROJECT_ROOT = (Get-Location).Path
```

Main folders:

```text
backend       Spring Boot + Docker Compose for PostgreSQL
ai-engine     Flask AI service and trained model
frontend-web  Static HTML/JS dashboard
```

## Part 2: Booting Up the System

Start the services in this order:

1. PostgreSQL database
2. Flask AI service
3. Spring Boot backend
4. Frontend dashboard

Use a separate PowerShell terminal for each long-running service.

## Step 1: Start PostgreSQL with Docker Compose

Terminal 1:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "backend")
docker compose up -d
docker ps --filter name=smart_irrigation_db
```

Expected:

```text
smart_irrigation_db ... 0.0.0.0:5436->5432/tcp
```

Verify PostgreSQL login:

```powershell
docker exec smart_irrigation_db psql -U postgres -d smart_irrigation_db -c "SELECT current_database(), current_user;"
```

Expected:

```text
smart_irrigation_db | postgres
```

Database connection used by Spring Boot:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5436/smart_irrigation_db
spring.datasource.username=postgres
spring.datasource.password=1234
```

Optional pgAdmin:

```text
URL:      http://localhost:5050
Email:    admin@admin.com
Password: admin
```

## Step 2: Start the Flask AI Service

Terminal 2:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "ai-engine")
```

Create a virtual environment:

```powershell
py -m venv .venv
```

If `py` is not available:

```powershell
python -m venv .venv
```

Activate it:

```powershell
.\.venv\Scripts\Activate.ps1
```

If PowerShell blocks activation, run:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
```

Install dependencies:

```powershell
python -m pip install --upgrade pip
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

Direct AI health test from another terminal:

```powershell
Invoke-RestMethod `
  -Uri "http://127.0.0.1:5000/predict" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"moisture":20,"is_raining":false}' |
  ConvertTo-Json -Depth 10
```

Expected response contains:

```json
{
  "decision": "IRRIGATE"
}
```

The exact decision may vary depending on the trained model and weather fallback/API result.

## Step 3: Start the Spring Boot Backend

Terminal 3:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "backend")
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Expected:

```text
Started BackendApplication
Tomcat started on port 8081
```

Backend URLs:

```text
Swagger UI: http://localhost:8081/swagger-ui.html
API base:   http://localhost:8081
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

## Step 4: Serve the Frontend Dashboard

Terminal 4:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "frontend-web")
node .\serve-static.js
```

Expected:

```text
Frontend dashboard running at http://localhost:5500
```

Open:

```text
http://localhost:5500
```

The dashboard reads backend data from:

```text
http://localhost:8081/api/v1/sensors/history
http://localhost:8081/api/v1/irrigation/history
```

## Part 3: End-to-End Testing Flow

Open a new PowerShell terminal for these tests.

```powershell
$BASE = "http://localhost:8081"
```

## Test 1: Send Mock Sensor Data to Spring Boot

This simulates the ESP32 sending sensor readings.

Low moisture, no rain:

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

- HTTP request succeeds.
- Response contains the saved sensor reading.
- Response includes an `id`.
- Backend console should show Hibernate insert SQL.
- Flask terminal should receive a `/predict` request.

## Test 2: Verify AI Forwarding and Decision Storage

Spring Boot forwards the sensor data to Flask AI from `SensorController`.

Check the Flask terminal. You should see a request similar to:

```text
POST /predict HTTP/1.1 200
```

Now poll the backend command endpoint:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Possible expected results:

If AI returned `IRRIGATE`:

```json
{
  "duration": 15,
  "reason": "AI decision: IRRIGATE",
  "hasCommand": true,
  "action": "start"
}
```

If AI returned `WAIT`:

```json
{
  "duration": 0,
  "reason": "AI decision: WAIT",
  "hasCommand": false,
  "action": "none"
}
```

If the AI service was offline:

```json
{
  "duration": 0,
  "reason": "AI decision: AI_SERVICE_UNAVAILABLE",
  "hasCommand": false,
  "action": "none"
}
```

## Test 3: Verify Consume-on-Read for ESP32 Polling

If the previous command returned `start`, immediately poll again:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected second poll:

```json
{
  "duration": 0,
  "reason": "AI decision: IDLE",
  "hasCommand": false,
  "action": "none"
}
```

This proves the infinite irrigation bug is fixed.

## Test 4: Manual Override Start Command

Queue a manual `start` command:

```powershell
Invoke-RestMethod `
  -Uri "$BASE/api/v1/irrigation/set-command" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"action":"start","duration":5,"reason":"Manual E2E test"}' |
  ConvertTo-Json -Depth 10
```

Expected:

```json
{
  "queued": true,
  "duration": 5,
  "reason": "Manual E2E test",
  "action": "start"
}
```

ESP32 first poll:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected:

```json
{
  "duration": 5,
  "reason": "Manual E2E test",
  "hasCommand": true,
  "action": "start"
}
```

ESP32 second poll:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected:

```json
{
  "duration": 0,
  "hasCommand": false,
  "action": "none"
}
```

## Test 5: Manual Override Stop Command

Queue a manual `stop` command:

```powershell
Invoke-RestMethod `
  -Uri "$BASE/api/v1/irrigation/set-command" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"action":"stop","duration":0,"reason":"Manual E2E stop test"}' |
  ConvertTo-Json -Depth 10
```

ESP32 first poll:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected:

```json
{
  "duration": 0,
  "reason": "Manual E2E stop test",
  "hasCommand": true,
  "action": "stop"
}
```

ESP32 second poll:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/command" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected:

```json
{
  "duration": 0,
  "hasCommand": false,
  "action": "none"
}
```

## Test 6: Log Pump Action

Simulate ESP32 reporting that the pump started:

```powershell
Invoke-RestMethod `
  -Uri "$BASE/api/v1/irrigation/log" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"pumpStatus":"ON","durationInMinutes":5}' |
  ConvertTo-Json -Depth 10
```

Verify irrigation history:

```powershell
Invoke-RestMethod -Uri "$BASE/api/v1/irrigation/history?page=0&size=10" -Method Get |
  ConvertTo-Json -Depth 10
```

Expected:

- `content` contains the new irrigation log.
- `pumpStatus` is `ON`.
- `durationInMinutes` is `5`.

## Test 7: Verify PostgreSQL Tables

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

## Test 8: Frontend Dashboard Verification

Open:

```text
http://localhost:5500
```

You should see:

- Backend API card showing `http://localhost:8081`
- Connection badge should become `Live Data`
- Latest Status should show the newest moisture reading
- Moisture History chart should display recent moisture values
- Recent Irrigation Logs should show pump activity after calling `/api/v1/irrigation/log`

If the dashboard shows `Connection Error`:

1. Confirm backend is running:

```powershell
Invoke-WebRequest -UseBasicParsing "http://localhost:8081/swagger-ui.html" |
  Select-Object StatusCode
```

2. Confirm frontend is served from:

```text
http://localhost:5500
```

3. Confirm CORS includes port `5500` in `backend/src/main/resources/application.properties`.

## One-Shot E2E Smoke Test Script

You can paste this into PowerShell after all services are running:

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

Write-Host "`n[6] Queue manual start"
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

Write-Host "`n[9] Log pump action"
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

## Shutdown Commands

Stop Spring Boot and Flask with `Ctrl+C` in their terminals.

Stop frontend with `Ctrl+C`.

Stop Docker services:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "backend")
docker compose down
```

To remove the PostgreSQL volume and reset all database data:

```powershell
docker compose down -v
```

Use `down -v` only when you intentionally want a clean database.

## Final Pre-Push Checklist

Run this before committing:

```powershell
Set-Location (Join-Path $PROJECT_ROOT "backend")
rg -n "<<<<<<<|=======|>>>>>>>" .
.\mvnw.cmd test
```

Expected:

- `rg` prints no conflict markers.
- Maven ends with `BUILD SUCCESS`.

Then check Git status:

```powershell
Set-Location $PROJECT_ROOT
git status --short
```

Review the changed files, then commit.
