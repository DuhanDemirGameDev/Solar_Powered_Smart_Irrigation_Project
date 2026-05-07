package com.example.smartirrigationmobile.model;

import com.google.gson.annotations.SerializedName;

public class AiPredictionRequest {

    private final double moisture;

    @SerializedName("is_raining")
    private final boolean isRaining;

    public AiPredictionRequest(double moisture, boolean isRaining) {
        this.moisture = moisture;
        this.isRaining = isRaining;
    }

    public double getMoisture() {
        return moisture;
    }

    public boolean isRaining() {
        return isRaining;
    }
}
