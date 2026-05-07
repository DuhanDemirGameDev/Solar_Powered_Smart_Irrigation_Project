package com.example.smartirrigationmobile.model;

public class PumpCommandRequest {

    private String action;
    private Integer duration;
    private String reason;

    public PumpCommandRequest(String action, Integer duration, String reason) {
        this.action = action;
        this.duration = duration;
        this.reason = reason;
    }

    public String getAction() {
        return action;
    }

    public Integer getDuration() {
        return duration;
    }

    public String getReason() {
        return reason;
    }
}
