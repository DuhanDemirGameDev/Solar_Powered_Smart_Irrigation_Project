import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
import pickle
import os

# Define paths
base_dir = os.path.dirname(os.path.abspath(__file__))
DATASET_PATH = os.path.join(base_dir, "..", "model", "dataset.csv")
MODEL_PATH = os.path.join(base_dir, "..", "model", "irrigation_model.pkl")

print("Working directory:", os.getcwd())
print("dataset.csv exists:", os.path.exists(DATASET_PATH))

# Load dataset
df = pd.read_csv(DATASET_PATH)

# Features and target
X = df[["moisture", "rain_prob", "is_raining", "temperature", "humidity"]]
y = df["decision"]

# Split into training and testing sets
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Train the model
model = RandomForestClassifier(
    n_estimators=100,
    random_state=42,
    class_weight="balanced"  # Gives more weight to minority classes
)
model.fit(X_train, y_train)

# Evaluate model performance
y_pred = model.predict(X_test)
print(classification_report(y_test, y_pred))

# Test with sample inputs
print("\nSample predictions")
samples = [
    [35, 55, 0, 28, 60],   # mid-range
    [20, 20, 0, 15, 40],    # irrigation case
    [80, 30, 0, 25, 50],   # no irrigation
    [50, 75, 0, 25, 40],   # postpone
]
for sample in samples:
    sample_df = pd.DataFrame([sample], columns=["moisture", "rain_prob", "is_raining", "temperature", "humidity"])
    prediction = model.predict(sample_df)[0]
    print(f"moisture:{sample[0]} rain_prob:{sample[1]} is_raining:{sample[2]} temperature:{sample[3]} → {prediction}")

# Save the model
with open(MODEL_PATH, "wb") as f:
    pickle.dump(model, f)

print("\nModel saved:", MODEL_PATH)