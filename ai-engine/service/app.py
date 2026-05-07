import os
import pickle

import pandas as pd
import requests
from flask import Flask, jsonify, request
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

base_dir = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(base_dir, "..", "model", "irrigation_model.pkl")

with open(MODEL_PATH, "rb") as f:
    model = pickle.load(f)

API_KEY = "cd8847e2ab961cb4f9c533c7aa6de1df"


@app.route("/predict", methods=["POST"])
def predict():
    body = request.get_json(silent=True) or {}

    moisture = body.get("moisture")
    if moisture is None:
        return jsonify({"error": "moisture is required"}), 400

    try:
        moisture = float(moisture)
    except (TypeError, ValueError):
        return jsonify({"error": "moisture must be numeric"}), 400

    if moisture < 0 or moisture > 100:
        return jsonify({"error": "moisture must be between 0 and 100"}), 400

    is_raining = body.get("is_raining", False)
    if isinstance(is_raining, str):
        is_raining = is_raining.strip().lower() in ("true", "1", "yes", "y", "on")
    else:
        is_raining = bool(is_raining)

    lat, lon = 39.9334, 32.8597
    url = (
        "https://api.openweathermap.org/data/2.5/forecast"
        f"?lat={lat}&lon={lon}&appid={API_KEY}&units=metric&lang=tr"
    )

    try:
        response = requests.get(url, timeout=10)
        response.raise_for_status()
        data = response.json()
        first = data["list"][0]

        rain_prob = first["pop"] * 100
        temperature = first["main"]["temp"]
        humidity = first["main"]["humidity"]

    except requests.exceptions.ConnectionError:
        print("No internet connection, using default values")
        rain_prob, temperature, humidity = 0, 20, 50

    except requests.exceptions.Timeout:
        print("API timeout, using default values")
        rain_prob, temperature, humidity = 0, 20, 50

    except Exception as e:
        print(f"Unexpected weather/model input error: {e}")
        rain_prob, temperature, humidity = 0, 20, 50

    input_data = pd.DataFrame(
        [[moisture, rain_prob, int(is_raining), temperature, humidity]],
        columns=["moisture", "rain_prob", "is_raining", "temperature", "humidity"],
    )

    try:
        decision = model.predict(input_data)[0]
    except Exception as e:
        print(f"Model prediction error: {e}")
        return jsonify({"error": "model prediction failed"}), 500

    return jsonify(
        {
            "decision": str(decision),
            "moisture": moisture,
            "rain_prob": rain_prob,
            "temperature": temperature,
            "humidity": humidity,
            "is_raining": is_raining,
        }
    )


if __name__ == "__main__":
    app.run(debug=True)
