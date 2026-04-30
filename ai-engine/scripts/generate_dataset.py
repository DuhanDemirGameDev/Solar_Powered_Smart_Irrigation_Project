import pandas as pd
import random
import os

random.seed(42)

data = []

for _ in range(1000):
    moisture = random.randint(0, 100) + random.uniform(-5, 5)
    rain_prob = random.randint(0, 100) + random.uniform(-5, 5)
    temperature = round(random.uniform(0, 40), 1)
    humidity = random.randint(20, 100)

    moisture = max(0, min(100, moisture))
    rain_prob = max(0, min(100, rain_prob))

    is_raining = random.random() < 0.3

    if is_raining:
        decision = "POSTPONE"

    elif moisture > 70:
        decision = "NO_IRRIGATION" if random.random() < 0.9 else "WAIT"

    elif rain_prob > 60:
        decision = "POSTPONE" if random.random() < 0.9 else "WAIT"

    elif moisture < 30 and rain_prob < 30:
        # Hot + dry conditions → more aggressive irrigation
        if temperature > 30 and humidity < 40:
            decision = "IRRIGATE" if random.random() < 0.97 else "WAIT"
        else:
            decision = "IRRIGATE" if random.random() < 0.9 else "WAIT"

    elif moisture < 45 and temperature > 28:
        # Middle moisture but very hot → still irrigate
        decision = "IRRIGATE" if random.random() < 0.7 else "WAIT"

    else:
        decision = "WAIT"

    data.append([moisture, rain_prob, is_raining, temperature, humidity, decision])

df = pd.DataFrame(data, columns=["moisture", "rain_prob", "is_raining", "temperature", "humidity", "decision"])
base_dir = os.path.dirname(os.path.abspath(__file__))
DATASET_PATH = os.path.join(base_dir, "..", "model", "dataset.csv")
df.to_csv(DATASET_PATH, index=False)

print(f"Dataset created: {len(df)} rows")
print(df["decision"].value_counts())