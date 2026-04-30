import requests
import pickle
import os
import pandas as pd
from flask import Flask, request, jsonify

app = Flask(__name__)

# Load model
base_dir = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(base_dir, "..", "model", "irrigation_model.pkl")

with open(MODEL_PATH, "rb") as f:
    model = pickle.load(f)

API_KEY = "cd8847e2ab961cb4f9c533c7aa6de1df"

@app.route("/predict", methods=["POST"])
def predict():
    body = request.get_json()

    moisture = body.get("moisture")
    is_raining = body.get("is_raining", False)
    
    if moisture is None:
        return jsonify({"error": "moisture is required"}), 400

    lat, lon = 39.9334, 32.8597  # Ankara
    url = f"https://api.openweathermap.org/data/2.5/forecast?lat={lat}&lon={lon}&appid={API_KEY}&units=metric&lang=tr"

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
        print(f"Unexpected error: {e}")
        rain_prob, temperature, humidity = 0, 20, 50

    input_data = pd.DataFrame(
        [[moisture, rain_prob, int(is_raining), temperature, humidity]],
        columns=["moisture", "rain_prob", "is_raining", "temperature", "humidity"]
    )
    decision = model.predict(input_data)[0]

    return jsonify({
        "decision": decision,
        "moisture": moisture,
        "rain_prob": rain_prob,
        "temperature": temperature,
        "humidity": humidity,
        "is_raining": is_raining
    })

if __name__ == "__main__":
    app.run(debug=True)