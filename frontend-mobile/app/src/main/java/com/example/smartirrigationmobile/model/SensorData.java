package com.example.smartirrigationmobile.model;

import com.google.gson.annotations.SerializedName;

public class SensorData {

    private Long id;

    @SerializedName("moisture_percent")
    private Double moisturePercent;

    @SerializedName("moisture_raw")
    private Integer moistureRaw;

    @SerializedName("is_raining")
    private Boolean isRaining;

    @SerializedName("rain_sensor_raw")
    private Integer rainSensorRaw;

    @SerializedName("pump_state")
    private String pumpState;

    @SerializedName("pump_remaining_time")
    private Integer pumpRemainingTime;

    private String timestamp;

    public SensorData() {
    }

    public Long getId() {
        return id;
    }

    public Double getMoisturePercent() {
        return moisturePercent;
    }

    public Integer getMoistureRaw() {
        return moistureRaw;
    }

    public Boolean getRaining() {
        return isRaining;
    }

    public Integer getRainSensorRaw() {
        return rainSensorRaw;
    }

    public String getPumpState() {
        return pumpState;
    }

    public Integer getPumpRemainingTime() {
        return pumpRemainingTime;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
