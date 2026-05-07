package com.example.smartirrigationmobile.model;

public class IrrigationLog {

    private Long id;
    private String pumpStatus;
    private Integer durationInMinutes;
    private String timestamp;

    public IrrigationLog() {
    }

    public Long getId() {
        return id;
    }

    public String getPumpStatus() {
        return pumpStatus;
    }

    public Integer getDurationInMinutes() {
        return durationInMinutes;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
