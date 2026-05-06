const API_BASE_URL = "http://localhost:8081";
const SENSOR_HISTORY_URL = `${API_BASE_URL}/api/v1/sensors/history?page=0&size=20`;
const IRRIGATION_HISTORY_URL = `${API_BASE_URL}/api/v1/irrigation/history?page=0&size=20`;

const latestMoistureEl = document.getElementById("latestMoisture");
const latestTimestampEl = document.getElementById("latestTimestamp");
const latestPumpStateEl = document.getElementById("latestPumpState");
const latestRainStatusEl = document.getElementById("latestRainStatus");
const connectionBadgeEl = document.getElementById("connectionBadge");
const statusErrorEl = document.getElementById("statusError");
const chartEmptyStateEl = document.getElementById("chartEmptyState");
const irrigationTableBodyEl = document.getElementById("irrigationTableBody");
const tableErrorEl = document.getElementById("tableError");

let moistureChart;

document.addEventListener("DOMContentLoaded", () => {
    initializeDashboard();
});

async function initializeDashboard() {
    connectionBadgeEl.textContent = "Connecting...";

    try {
        const [sensorHistory, irrigationLogs] = await Promise.all([
            fetchPageContent(SENSOR_HISTORY_URL),
            fetchPageContent(IRRIGATION_HISTORY_URL)
        ]);

        renderLatestStatus(sensorHistory);
        renderMoistureChart(sensorHistory);
        renderIrrigationTable(irrigationLogs);

        connectionBadgeEl.textContent = "Live Data";
    } catch (error) {
        console.error("Dashboard initialization failed:", error);
        connectionBadgeEl.textContent = "Connection Error";

        showError(
            statusErrorEl,
            "Unable to load dashboard data. Make sure the backend is running on http://localhost:8081 and CORS is configured for your frontend origin."
        );
        showError(
            tableErrorEl,
            "Irrigation logs could not be loaded from the backend."
        );
    }
}

async function fetchPageContent(url) {
    const response = await fetch(url, {
        method: "GET",
        headers: {
            "Accept": "application/json"
        }
    });

    if (!response.ok) {
        throw new Error(`Request failed with status ${response.status} for ${url}`);
    }

    const page = await response.json();
    return Array.isArray(page.content) ? page.content : [];
}

function renderLatestStatus(sensorHistory) {
    if (!sensorHistory.length) {
        latestMoistureEl.textContent = "--%";
        latestTimestampEl.textContent = "No sensor readings available yet.";
        latestPumpStateEl.textContent = "--";
        latestRainStatusEl.textContent = "--";
        return;
    }

    const latest = sensorHistory[0];

    latestMoistureEl.textContent = `${formatNumber(latest.moisture_percent)}%`;
    latestTimestampEl.textContent = `Last updated: ${formatDateTime(latest.timestamp)}`;
    latestPumpStateEl.textContent = latest.pump_state || "Unknown";
    latestRainStatusEl.textContent = latest.is_raining ? "Raining" : "Dry";
}

function renderMoistureChart(sensorHistory) {
    if (!sensorHistory.length) {
        chartEmptyStateEl.classList.remove("d-none");
        return;
    }

    const sortedHistory = [...sensorHistory].reverse();
    const labels = sortedHistory.map((entry) => formatChartLabel(entry.timestamp));
    const moistureValues = sortedHistory.map((entry) => entry.moisture_percent);
    const ctx = document.getElementById("moistureChart");

    moistureChart = new Chart(ctx, {
        type: "line",
        data: {
            labels,
            datasets: [
                {
                    label: "Moisture %",
                    data: moistureValues,
                    borderColor: "#1f7a4f",
                    backgroundColor: "rgba(31, 122, 79, 0.14)",
                    borderWidth: 3,
                    fill: true,
                    tension: 0.35,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    pointBackgroundColor: "#f4b942",
                    pointBorderColor: "#1f7a4f",
                    pointBorderWidth: 2
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: "index",
                intersect: false
            },
            plugins: {
                legend: {
                    display: true,
                    labels: {
                        usePointStyle: true
                    }
                },
                tooltip: {
                    callbacks: {
                        label(context) {
                            return ` ${context.parsed.y}%`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100,
                    ticks: {
                        callback(value) {
                            return `${value}%`;
                        }
                    }
                },
                x: {
                    ticks: {
                        maxRotation: 0,
                        autoSkip: true,
                        maxTicksLimit: 6
                    }
                }
            }
        }
    });
}

function renderIrrigationTable(irrigationLogs) {
    if (!irrigationLogs.length) {
        irrigationTableBodyEl.innerHTML = `
            <tr>
                <td colspan="3" class="text-center text-muted py-4">No irrigation logs available yet.</td>
            </tr>
        `;
        return;
    }

    irrigationTableBodyEl.innerHTML = irrigationLogs.map((log) => `
        <tr>
            <td>${formatDateTime(log.timestamp)}</td>
            <td>
                <span class="badge ${log.pumpStatus === "ON" ? "text-bg-success" : "text-bg-secondary"}">
                    ${escapeHtml(log.pumpStatus || "UNKNOWN")}
                </span>
            </td>
            <td>${formatDuration(log.durationInMinutes)}</td>
        </tr>
    `).join("");
}

function formatDateTime(timestamp) {
    if (!timestamp) {
        return "N/A";
    }

    return new Date(timestamp).toLocaleString("en-US", {
        year: "numeric",
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

function showError(element, message) {
    element.textContent = message;
    element.classList.remove("d-none");
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}
