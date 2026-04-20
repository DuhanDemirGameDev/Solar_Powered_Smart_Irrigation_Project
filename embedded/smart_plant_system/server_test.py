from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse
import uvicorn

app = FastAPI()

# Sensör verilerini saklayacağımız yer
latest_data = {
    "moisture_percent": 0.0,
    "moisture_raw": 0,
    "is_raining": False,
    "pump_state": "IDLE",
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

# 4. Web sayfası (HTML)
@app.get("/", response_class=HTMLResponse)
async def home():
    rain_text = "Yağmur Yağıyor 🌧️" if latest_data['is_raining'] else "Hava Açık ☀️"
    return f"""
    <html>
        <head>
            <title>Akıllı Sulama Test Paneli</title>
            <meta http-equiv="refresh" content="3"> 
            <style>
                body {{ font-family: 'Segoe UI', sans-serif; text-align: center; background-color: #f0f2f5; padding-top: 50px; }}
                .container {{ background: white; padding: 30px; border-radius: 20px; display: inline-block; box-shadow: 0 10px 20px rgba(0,0,0,0.1); }}
                .moisture {{ font-size: 80px; color: #3498db; font-weight: bold; }}
                .btn {{ padding: 12px 24px; font-size: 16px; font-weight: bold; margin: 10px; cursor: pointer; border: none; border-radius: 8px; color: white; transition: 0.2s; }}
                .btn-start {{ background-color: #2ecc71; }}
                .btn-start:hover {{ background-color: #27ae60; }}
                .btn-stop {{ background-color: #e74c3c; }}
                .btn-stop:hover {{ background-color: #c0392b; }}
            </style>
            <script>
                // Butonlara basıldığında Python'a komut ileten JavaScript fonksiyonu
                function sendCommand(action) {{
                    fetch('/api/set-command', {{
                        method: 'POST',
                        headers: {{ 'Content-Type': 'application/json' }},
                        body: JSON.stringify({{ action: action }})
                    }}).then(response => console.log(action + " komutu gönderildi."));
                }}
            </script>
        </head>
        <body>
            <div class="container">
                <h1>Sistem Test Paneli</h1>
                <div class="moisture">%{latest_data['moisture_percent']}</div>
                <p>Toprak Nem Seviyesi</p>
                <hr>
                <h3>{rain_text}</h3>
                <p>Pompa Durumu: <b>{latest_data['pump_state']}</b></p>
                <p style="font-size: 12px; color: gray;">Ham Değer: {latest_data['moisture_raw']} | Durum: {latest_data['status']}</p>
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