package com.example.smartirrigationmobile.model;

import com.google.gson.annotations.SerializedName;

public class AiPredictionResponse {

    private String decision;
    private Double moisture;

    @SerializedName("rain_prob")
    private Double rainProbability;

    private Double temperature;
    private Double humidity;

    @SerializedName("is_raining")
    private Boolean isRaining;

    public String getDecision() {
        return decision;
    }

    public Double getMoisture() {
        return moisture;
    }

    public Double getRainProbability() {
        return rainProbability;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public Boolean getRaining() {
        return isRaining;
    }
}
