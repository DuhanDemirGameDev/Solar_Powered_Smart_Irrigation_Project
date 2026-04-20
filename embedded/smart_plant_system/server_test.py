from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse
import uvicorn

app = FastAPI()

# Sensör verilerini saklayacağımız yer
latest_data = {
    "moisture_percent": 0.0,
    "moisture_raw": 0,
    "is_raining": False,
    "rain_sensor_raw": 0,
    "pump_state": "IDLE",
    "pump_remaining_time": 0,
    "status": "ESP32 bekleniyor..."
}

# YENİ: Bekleyen röle komutunu tutacağımız yer
pending_command = {
    "action": "none",
    "duration": 0,
    "reason": ""
}

# 1. ESP32'nin veri gönderdiği POST ucu (Sensörlerden gelen veri)
@app.post("/api/sensor-data")
async def receive_data(request: Request):
    global latest_data
    data = await request.json()
    latest_data.update(data)
    latest_data["status"] = "Veri Akışı Aktif ✅"
    print(f"Gelen Veri: {data}")
    return {"status": "success"}

# 2. YENİ: ESP32'nin komut almaya geldiği GET ucu
@app.get("/api/pump-command")
async def get_command():
    global pending_command
    # Mevcut komutu ESP32'ye gönderilmek üzere kopyala
    cmd_to_send = pending_command.copy()
    
    # Eğer "none" dışında bir komut gönderiyorsak, gönderdikten sonra sıfırla. 
    # (Böylece ESP32 bir sonraki soruşunda aynı komutu tekrar çalıştırmaz)
    if pending_command["action"] != "none":
        print(f"ESP32'ye Komut İletildi: {cmd_to_send}")
        pending_command = {"action": "none", "duration": 0, "reason": ""}
        
    return cmd_to_send

# 3. YENİ: Web sayfasındaki butonlardan gelen isteği yakalayan POST ucu
@app.post("/api/set-command")
async def set_command(request: Request):
    global pending_command
    data = await request.json()
    action = data.get("action", "none")

    # Gelen isteğe göre komutu ayarla
    if action == "start":
        # 10 saniye boyunca çalıştır (Süreyi isteğine göre değiştirebilirsin)
        pending_command = {"action": "start", "duration": 10, "reason": "Web panelden manuel başlatma"}
    elif action == "stop":
        pending_command = {"action": "stop", "duration": 0, "reason": "Web panelden manuel durdurma"}

    print(f"Yeni komut kuyruğa eklendi: {pending_command}")
    return {"status": "ok"}

# 4. Canlı sensör verisi API (JavaScript AJAX ile çekilecek)
@app.get("/api/live-data")
async def live_data():
    return latest_data

# 5. Web sayfası (HTML) — artık JavaScript ile otomatik güncelleniyor, meta refresh YOK
@app.get("/", response_class=HTMLResponse)
async def home():
    return """
    <html>
        <head>
            <title>Akıllı Sulama Test Paneli</title>
            <style>
                body { font-family: 'Segoe UI', sans-serif; text-align: center; background-color: #f0f2f5; padding-top: 50px; }
                .container { background: white; padding: 30px; border-radius: 20px; display: inline-block; box-shadow: 0 10px 20px rgba(0,0,0,0.1); min-width: 400px; }
                .moisture { font-size: 80px; color: #3498db; font-weight: bold; }
                .raw-info { font-size: 12px; color: gray; }
                .pump-running { color: #2ecc71; font-weight: bold; }
                .pump-idle { color: #95a5a6; }
                .pump-cooldown { color: #f39c12; }
                .remaining { font-size: 24px; color: #e67e22; font-weight: bold; margin: 10px 0; }
                .btn { padding: 12px 24px; font-size: 16px; font-weight: bold; margin: 10px; cursor: pointer; border: none; border-radius: 8px; color: white; transition: 0.2s; }
                .btn-start { background-color: #2ecc71; }
                .btn-start:hover { background-color: #27ae60; }
                .btn-stop { background-color: #e74c3c; }
                .btn-stop:hover { background-color: #c0392b; }
                .btn:disabled { background-color: #bdc3c7; cursor: not-allowed; }
                .status-dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 5px; }
                .dot-active { background-color: #2ecc71; animation: pulse 1s infinite; }
                .dot-inactive { background-color: #e74c3c; }
                @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
            </style>
            <script>
                function sendCommand(action) {
                    fetch('/api/set-command', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ action: action })
                    }).then(response => console.log(action + " komutu gönderildi."));
                }

                // Her 2 saniyede bir verileri AJAX ile çek (sayfa yenilenmez, pompa kesilmez)
                function updateData() {
                    fetch('/api/live-data')
                        .then(r => r.json())
                        .then(data => {
                            document.getElementById('moisture-val').textContent = '%' + data.moisture_percent;
                            document.getElementById('raw-val').textContent = 'Ham Değer: ' + data.moisture_raw;
                            document.getElementById('rain-text').textContent = data.is_raining ? 'Yağmur Yağıyor 🌧️' : 'Hava Açık ☀️';
                            
                            let pumpEl = document.getElementById('pump-state');
                            pumpEl.textContent = data.pump_state;
                            pumpEl.className = data.pump_state === 'RUNNING' ? 'pump-running' :
                                               data.pump_state === 'COOLDOWN' ? 'pump-cooldown' : 'pump-idle';

                            let remainEl = document.getElementById('remaining');
                            if (data.pump_state === 'RUNNING' && data.pump_remaining_time > 0) {
                                remainEl.textContent = 'Kalan süre: ' + data.pump_remaining_time + 's';
                                remainEl.style.display = 'block';
                            } else {
                                remainEl.style.display = 'none';
                            }

                            let dotEl = document.getElementById('status-dot');
                            dotEl.className = 'status-dot ' + (data.status.includes('Aktif') ? 'dot-active' : 'dot-inactive');
                            document.getElementById('status-text').textContent = data.status;
                        })
                        .catch(err => console.log('Veri çekilemedi:', err));
                }

                // Sayfa yüklenince hemen bir kez çek, sonra 2 saniyede bir tekrarla
                window.onload = function() {
                    updateData();
                    setInterval(updateData, 2000);
                };
            </script>
        </head>
        <body>
            <div class="container">
                <h1>Sistem Test Paneli</h1>
                <div class="moisture" id="moisture-val">%0</div>
                <p>Toprak Nem Seviyesi</p>
                <hr>
                <h3 id="rain-text">Yükleniyor...</h3>
                <p>Pompa Durumu: <b id="pump-state" class="pump-idle">IDLE</b></p>
                <div class="remaining" id="remaining" style="display: none;"></div>
                <p class="raw-info" id="raw-val">Ham Değer: -</p>
                <p class="raw-info"><span class="status-dot dot-inactive" id="status-dot"></span><span id="status-text">Bekleniyor...</span></p>
                <hr>
                <h3>Manuel Kontrol</h3>
                <button class="btn btn-start" onclick="sendCommand('start')">Pompayı Başlat (10s)</button>
                <button class="btn btn-stop" onclick="sendCommand('stop')">Pompayı Durdur</button>
            </div>
        </body>
    </html>
    """

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)