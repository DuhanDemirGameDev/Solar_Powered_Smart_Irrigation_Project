package com.example.smartirrigationmobile.model;

public class ManualCommandResponse {

    private Boolean queued;
    private String action;
    private Integer duration;
    private String unit;
    private String reason;

    public ManualCommandResponse() {
    }

    public Boolean getQueued() {
        return queued;
    }

    public String getAction() {
        return action;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getUnit() {
        return unit;
    }

    public String getReason() {
        return reason;
    }
}
