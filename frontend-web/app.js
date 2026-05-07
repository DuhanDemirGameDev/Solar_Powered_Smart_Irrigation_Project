const API_BASE_URL = "http://localhost:8081";
const SENSOR_HISTORY_URL = `${API_BASE_URL}/api/v1/sensors/history?page=0&size=20`;
const IRRIGATION_HISTORY_URL = `${API_BASE_URL}/api/v1/irrigation/history?page=0&size=20`;
const MANUAL_PUMP_URL = `${API_BASE_URL}/api/v1/irrigation/set-command`;
const POLL_INTERVAL_MS = 10000;

const elements = {
    latestMoisture: document.getElementById("latestMoisture"),
    latestTimestamp: document.getElementById("latestTimestamp"),
    latestPumpState: document.getElementById("latestPumpState"),
    latestRainStatus: document.getElementById("latestRainStatus"),
    latestRawMoisture: document.getElementById("latestRawMoisture"),
    moistureRing: document.getElementById("moistureRingFill")?.parentElement,
    pumpChip: document.getElementById("pumpChip"),
    connectionBadge: document.getElementById("connectionBadge"),
    lastRefreshText: document.getElementById("lastRefreshText"),
    sampleCount: document.getElementById("sampleCount"),
    systemMode: document.getElementById("systemMode"),
    lastPumpLog: document.getElementById("lastPumpLog"),
    chartAlert: document.getElementById("chartAlert"),
    logAlert: document.getElementById("logAlert"),
    chartEmptyState: document.getElementById("chartEmptyState"),
    irrigationTableBody: document.getElementById("irrigationTableBody"),
    manualPumpBtn: document.getElementById("manualPumpBtn"),
    toastRegion: document.getElementById("toastRegion"),
    moistureChartCanvas: document.getElementById("moistureChart")
};

let moistureChart = null;
let refreshTimer = null;
let isRefreshing = false;

document.addEventListener("DOMContentLoaded", () => {
    elements.manualPumpBtn?.addEventListener("click", handleManualPumpClick);
    refreshDashboard({ showLoadingState: true });
    refreshTimer = window.setInterval(() => refreshDashboard(), POLL_INTERVAL_MS);
});

window.addEventListener("beforeunload", () => {
    if (refreshTimer) {
        window.clearInterval(refreshTimer);
    }

    destroyMoistureChart();
});

async function refreshDashboard(options = {}) {
    if (isRefreshing) {
        return;
    }

    isRefreshing = true;

    if (options.showLoadingState) {
        setConnectionState("connecting");
    }

    try {
        const [sensorHistory, irrigationLogs] = await Promise.all([
            fetchPageContent(SENSOR_HISTORY_URL),
            fetchPageContent(IRRIGATION_HISTORY_URL)
        ]);

        renderLatestStatus(sensorHistory);
        renderMoistureChart(sensorHistory);
        renderIrrigationTable(irrigationLogs);
        renderSummaryCards(sensorHistory, irrigationLogs);
        setConnectionState("live");
        hideInlineAlert(elements.chartAlert);
        hideInlineAlert(elements.logAlert);
    } catch (error) {
        console.error("Dashboard refresh failed:", error);
        setConnectionState("error");
        showInlineAlert(elements.chartAlert, "Live data could not be refreshed. The last successful values remain visible.");
        showInlineAlert(elements.logAlert, "Irrigation logs are temporarily unavailable.");
        showToast("Connection issue", "Could not reach the backend on localhost:8081.", "error");
    } finally {
        isRefreshing = false;
    }
}

async function fetchPageContent(url) {
    const response = await fetch(url, {
        method: "GET",
        headers: {
            Accept: "application/json"
        }
    });

    if (!response.ok) {
        throw new Error(`GET ${url} failed with HTTP ${response.status}`);
    }

    const page = await response.json();
    return Array.isArray(page.content) ? page.content : [];
}

function renderLatestStatus(sensorHistory) {
    const latest = sensorHistory[0];

    if (!latest) {
        elements.latestMoisture.textContent = "--%";
        elements.latestTimestamp.textContent = "No sensor readings available yet.";
        elements.latestPumpState.textContent = "--";
        elements.latestRainStatus.textContent = "--";
        elements.latestRawMoisture.textContent = "--";
        updatePumpChip("--");
        updateMoistureRing(null);
        return;
    }

    const moisture = readField(latest, "moisturePercent", "moisture_percent");
    const pumpState = normalizePumpState(readField(latest, "pumpState", "pump_state"));
    const isRaining = Boolean(readField(latest, "isRaining", "is_raining"));
    const rawMoisture = readField(latest, "moistureRaw", "moisture_raw");

    elements.latestMoisture.textContent = `${formatNumber(moisture)}%`;
    elements.latestTimestamp.textContent = `Last updated ${formatDateTime(latest.timestamp)}`;
    elements.latestPumpState.textContent = pumpState;
    elements.latestRainStatus.textContent = isRaining ? "Raining" : "Dry";
    elements.latestRawMoisture.textContent = rawMoisture ?? "--";

    updatePumpChip(pumpState);
    updateMoistureRing(moisture);
}

function renderMoistureChart(sensorHistory) {
    destroyMoistureChart();

    if (!sensorHistory.length) {
        elements.chartEmptyState.classList.remove("hidden");
        return;
    }

    elements.chartEmptyState.classList.add("hidden");

    const sortedHistory = [...sensorHistory].reverse();
    const labels = sortedHistory.map((entry) => formatChartLabel(entry.timestamp));
    const moistureValues = sortedHistory.map((entry) => readField(entry, "moisturePercent", "moisture_percent"));

    moistureChart = new Chart(elements.moistureChartCanvas, {
        type: "line",
        data: {
            labels,
            datasets: [
                {
                    label: "Moisture %",
                    data: moistureValues,
                    borderColor: "#176b47",
                    backgroundColor: createChartGradient(),
                    borderWidth: 3,
                    fill: true,
                    tension: 0.38,
                    pointRadius: 3,
                    pointHoverRadius: 6,
                    pointBackgroundColor: "#f5b642",
                    pointBorderColor: "#ffffff",
                    pointBorderWidth: 2
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: 550,
                easing: "easeOutQuart"
            },
            interaction: {
                mode: "index",
                intersect: false
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: "#14251d",
                    padding: 12,
                    displayColors: false,
                    callbacks: {
                        label(context) {
                            return `Moisture: ${formatNumber(context.parsed.y)}%`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100,
                    grid: {
                        color: "rgba(105, 118, 111, 0.14)"
                    },
                    ticks: {
                        color: "#69766f",
                        callback(value) {
                            return `${value}%`;
                        }
                    }
                },
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: "#69766f",
                        maxRotation: 0,
                        autoSkip: true,
                        maxTicksLimit: 6
                    }
                }
            }
        }
    });
}

function destroyMoistureChart() {
    if (moistureChart) {
        moistureChart.destroy();
        moistureChart = null;
    }
}

function createChartGradient() {
    const context = elements.moistureChartCanvas.getContext("2d");
    const gradient = context.createLinearGradient(0, 0, 0, 360);
    gradient.addColorStop(0, "rgba(31, 138, 192, 0.24)");
    gradient.addColorStop(0.48, "rgba(23, 107, 71, 0.12)");
    gradient.addColorStop(1, "rgba(23, 107, 71, 0)");
    return gradient;
}

function renderIrrigationTable(irrigationLogs) {
    if (!irrigationLogs.length) {
        elements.irrigationTableBody.innerHTML = `
            <tr>
                <td colspan="3" class="table-empty">No irrigation logs available yet.</td>
            </tr>
        `;
        return;
    }

    elements.irrigationTableBody.innerHTML = irrigationLogs.map((log) => {
        const status = String(log.pumpStatus || "UNKNOWN").toUpperCase();
        const statusClass = status === "ON" ? "on" : "off";

        return `
            <tr>
                <td>${formatDateTime(log.timestamp)}</td>
                <td><span class="state-badge ${statusClass}">${escapeHtml(status)}</span></td>
                <td>${formatDuration(log.durationInMinutes)}</td>
            </tr>
        `;
    }).join("");
}

function renderSummaryCards(sensorHistory, irrigationLogs) {
    elements.sampleCount.textContent = sensorHistory.length ? String(sensorHistory.length) : "--";

    const lastLog = irrigationLogs[0];
    if (!lastLog) {
        elements.lastPumpLog.textContent = "--";
        return;
    }

    elements.lastPumpLog.textContent = `${lastLog.pumpStatus || "UNKNOWN"} - ${formatDuration(lastLog.durationInMinutes)}`;
}

async function handleManualPumpClick() {
    const button = elements.manualPumpBtn;
    const originalText = button.innerHTML;

    button.disabled = true;
    button.innerHTML = `<span class="button-icon" aria-hidden="true">...</span><span>Sending command</span>`;

    try {
        const response = await fetch(MANUAL_PUMP_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            body: JSON.stringify({
                action: "start",
                duration: 15,
                reason: "Manual UI Override"
            })
        });

        if (!response.ok) {
            throw new Error(`Manual pump command failed with HTTP ${response.status}`);
        }

        showToast("Pump command queued", "Manual override sent for 15 seconds.", "success");
        await refreshDashboard();
    } catch (error) {
        console.error("Manual pump command failed:", error);
        showToast("Command failed", "The backend did not accept the manual pump command.", "error");
    } finally {
        window.setTimeout(() => {
            button.disabled = false;
            button.innerHTML = originalText;
        }, 1800);
    }
}

function setConnectionState(state) {
    elements.connectionBadge.classList.toggle("error", state === "error");

    if (state === "connecting") {
        elements.connectionBadge.innerHTML = `<span class="pulse-dot"></span>Connecting`;
        return;
    }

    if (state === "error") {
        elements.connectionBadge.innerHTML = `<span class="pulse-dot"></span>Offline`;
        return;
    }

    const now = new Date();
    elements.connectionBadge.innerHTML = `<span class="pulse-dot"></span>Live`;
    elements.lastRefreshText.textContent = `Synced ${now.toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    })}`;
}

function updatePumpChip(pumpState) {
    const isOn = ["ON", "START", "STARTED", "RUNNING"].includes(String(pumpState).toUpperCase());

    elements.pumpChip.textContent = `Pump ${pumpState}`;
    elements.pumpChip.classList.toggle("off", !isOn);
}

function updateMoistureRing(moisture) {
    if (!elements.moistureRing || typeof moisture !== "number") {
        elements.moistureRing?.style.setProperty("--moisture-angle", "0deg");
        return;
    }

    const safeMoisture = Math.max(0, Math.min(100, moisture));
    elements.moistureRing.style.setProperty("--moisture-angle", `${safeMoisture * 3.6}deg`);
}

function showInlineAlert(element, message) {
    element.textContent = message;
    element.classList.remove("hidden");
}

function hideInlineAlert(element) {
    element.textContent = "";
    element.classList.add("hidden");
}

function showToast(title, message, type = "success") {
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <strong>${escapeHtml(title)}</strong>
        <p>${escapeHtml(message)}</p>
    `;

    elements.toastRegion.appendChild(toast);

    window.setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transform = "translateY(8px)";
        toast.style.transition = "opacity 180ms ease, transform 180ms ease";
    }, 3200);

    window.setTimeout(() => {
        toast.remove();
    }, 3450);
}

function normalizePumpState(value) {
    if (!value) {
        return "Unknown";
    }

    const normalized = String(value).trim().toUpperCase();
    if (["START", "STARTED", "RUNNING"].includes(normalized)) {
        return "ON";
    }

    if (["STOP", "STOPPED", "IDLE"].includes(normalized)) {
        return "OFF";
    }

    return normalized;
}

function formatDateTime(timestamp) {
    if (!timestamp) {
        return "N/A";
    }

    return new Date(timestamp).toLocaleString("en-US", {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function formatChartLabel(timestamp) {
    if (!timestamp) {
        return "N/A";
    }

    return new Date(timestamp).toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit"
    });
}

function formatDuration(minutes) {
    if (minutes == null) {
        return "N/A";
    }

    return `${minutes} min`;
}

function formatNumber(value) {
    return typeof value === "number" ? value.toFixed(1) : "--";
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
