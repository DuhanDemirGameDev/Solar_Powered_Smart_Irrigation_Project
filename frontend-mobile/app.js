const API_BASE_URL = "http://localhost:8081";
const SENSOR_HISTORY_URL = `${API_BASE_URL}/api/v1/sensors/history?page=0&size=20`;
const IRRIGATION_COMMAND_URL = `${API_BASE_URL}/api/v1/irrigation/set-command`;
const POLL_INTERVAL_MS = 10000;

const elements = {
    screens: document.querySelectorAll(".screen"),
    navItems: document.querySelectorAll(".nav-item"),
    connectionStatus: document.getElementById("connectionStatus"),
    moistureGauge: document.getElementById("moistureGauge"),
    moistureValue: document.getElementById("moistureValue"),
    pumpState: document.getElementById("pumpState"),
    rainState: document.getElementById("rainState"),
    rawMoisture: document.getElementById("rawMoisture"),
    syncText: document.getElementById("syncText"),
    waterNowBtn: document.getElementById("waterNowBtn"),
    wateringOverlay: document.getElementById("wateringOverlay"),
    toast: document.getElementById("toast"),
    miniChart: document.getElementById("miniChart"),
    historyList: document.getElementById("historyList")
};

let pollTimer = null;
let isPolling = false;
let toastTimer = null;

document.addEventListener("DOMContentLoaded", () => {
    elements.navItems.forEach((item) => {
        item.addEventListener("click", () => showScreen(item.dataset.target));
    });

    elements.waterNowBtn.addEventListener("pointerdown", playButtonRipple);
    elements.waterNowBtn.addEventListener("click", sendWaterCommand);

    refreshData(true);
    pollTimer = window.setInterval(() => refreshData(), POLL_INTERVAL_MS);
});

window.addEventListener("beforeunload", () => {
    if (pollTimer) {
        window.clearInterval(pollTimer);
    }
});

function showScreen(target) {
    elements.screens.forEach((screen) => {
        screen.classList.toggle("active", screen.dataset.screen === target);
    });

    elements.navItems.forEach((item) => {
        item.classList.toggle("active", item.dataset.target === target);
    });
}

async function refreshData(isInitialLoad = false) {
    if (isPolling) {
        return;
    }

    isPolling = true;

    if (isInitialLoad) {
        setConnectionState("connecting");
    }

    try {
        const sensorHistory = await fetchSensorHistory();
        renderDashboard(sensorHistory);
        renderHistory(sensorHistory);
        setConnectionState("live");
    } catch (error) {
        console.error("Mobile dashboard refresh failed:", error);
        setConnectionState("offline");
        showToast("Connection lost");
    } finally {
        isPolling = false;
    }
}

async function fetchSensorHistory() {
    const response = await fetch(SENSOR_HISTORY_URL, {
        method: "GET",
        headers: {
            Accept: "application/json"
        }
    });

    if (!response.ok) {
        throw new Error(`Sensor history failed with HTTP ${response.status}`);
    }

    const page = await response.json();
    return Array.isArray(page.content) ? page.content : [];
}

function renderDashboard(sensorHistory) {
    const latest = sensorHistory[0];

    if (!latest) {
        elements.moistureValue.textContent = "--";
        elements.pumpState.textContent = "--";
        elements.rainState.textContent = "--";
        elements.rawMoisture.textContent = "--";
        elements.syncText.textContent = "No sensor readings yet";
        updateGauge(null);
        return;
    }

    const moisture = readField(latest, "moisturePercent", "moisture_percent");
    const pumpState = normalizePumpState(readField(latest, "pumpState", "pump_state"));
    const isRaining = Boolean(readField(latest, "isRaining", "is_raining"));
    const rawMoisture = readField(latest, "moistureRaw", "moisture_raw");

    elements.moistureValue.textContent = formatMoisture(moisture);
    elements.pumpState.textContent = pumpState;
    elements.rainState.textContent = isRaining ? "Rain" : "Dry";
    elements.rawMoisture.textContent = rawMoisture ?? "--";
    elements.syncText.textContent = `Updated ${formatTime(latest.timestamp)}`;

    updateGauge(moisture);
}

function renderHistory(sensorHistory) {
    if (!sensorHistory.length) {
        elements.miniChart.innerHTML = "";
        elements.historyList.innerHTML = `<p class="empty-state">No sensor history yet.</p>`;
        return;
    }

    const ordered = [...sensorHistory].reverse();
    elements.miniChart.innerHTML = ordered.map((entry) => {
        const moisture = clampNumber(readField(entry, "moisturePercent", "moisture_percent"), 0, 100);
        return `<span style="height: ${Math.max(8, moisture)}%"></span>`;
    }).join("");

    elements.historyList.innerHTML = sensorHistory.slice(0, 12).map((entry) => {
        const moisture = readField(entry, "moisturePercent", "moisture_percent");
        const pumpState = normalizePumpState(readField(entry, "pumpState", "pump_state"));

        return `
            <article class="log-item">
                <div>
                    <strong>${escapeHtml(formatMoisture(moisture))}% Moisture</strong>
                    <span>${escapeHtml(formatDate(entry.timestamp))}</span>
                </div>
                <span>${escapeHtml(pumpState)}</span>
            </article>
        `;
    }).join("");
}

async function sendWaterCommand() {
    elements.waterNowBtn.disabled = true;
    elements.wateringOverlay.classList.remove("hidden");

    try {
        const response = await fetch(IRRIGATION_COMMAND_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            body: JSON.stringify({
                action: "start",
                duration: 15,
                unit: "seconds",
                reason: "Mobile manual override"
            })
        });

        if (!response.ok) {
            throw new Error(`Manual watering failed with HTTP ${response.status}`);
        }

        showToast("Watering started");
        await refreshData();
    } catch (error) {
        console.error("Manual watering failed:", error);
        showToast("Connection lost");
    } finally {
        window.setTimeout(() => {
            elements.waterNowBtn.disabled = false;
            elements.wateringOverlay.classList.add("hidden");
        }, 650);
    }
}

function playButtonRipple(event) {
    const rect = elements.waterNowBtn.getBoundingClientRect();
    elements.waterNowBtn.style.setProperty("--tap-x", `${event.clientX - rect.left}px`);
    elements.waterNowBtn.style.setProperty("--tap-y", `${event.clientY - rect.top}px`);
    elements.waterNowBtn.classList.remove("ripple");
    void elements.waterNowBtn.offsetWidth;
    elements.waterNowBtn.classList.add("ripple");
}

function setConnectionState(state) {
    const isOffline = state === "offline";
    elements.connectionStatus.classList.toggle("offline", isOffline);
    elements.connectionStatus.innerHTML = `<span></span>${isOffline ? "Offline" : state === "connecting" ? "Syncing" : "Live"}`;
}

function updateGauge(moisture) {
    const safeMoisture = typeof moisture === "number" ? clampNumber(moisture, 0, 100) : 0;
    elements.moistureGauge.style.setProperty("--moisture-angle", `${safeMoisture * 3.6}deg`);
}

function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.add("show");

    if (toastTimer) {
        window.clearTimeout(toastTimer);
    }

    toastTimer = window.setTimeout(() => {
        elements.toast.classList.remove("show");
    }, 2600);
}

function normalizePumpState(value) {
    if (!value) {
        return "Unknown";
    }

    const normalized = String(value).trim().toUpperCase();
    if (["START", "STARTED", "RUNNING", "ON"].includes(normalized)) {
        return "ON";
    }

    if (["STOP", "STOPPED", "IDLE", "OFF"].includes(normalized)) {
        return "OFF";
    }

    return normalized;
}

function formatMoisture(value) {
    return typeof value === "number" ? Math.round(value).toString() : "--";
}

function formatTime(timestamp) {
    if (!timestamp) {
        return "just now";
    }

    return new Date(timestamp).toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit"
    });
}

function formatDate(timestamp) {
    if (!timestamp) {
        return "No timestamp";
    }

    return new Date(timestamp).toLocaleString("en-US", {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function clampNumber(value, min, max) {
    const number = Number(value);

    if (Number.isNaN(number)) {
        return min;
    }

    return Math.max(min, Math.min(max, number));
}

function readField(item, camelCaseName, snakeCaseName) {
    return item?.[camelCaseName] ?? item?.[snakeCaseName];
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}
