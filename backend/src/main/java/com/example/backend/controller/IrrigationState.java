package com.example.backend.controller;

public class IrrigationState {
    public static final Object COMMAND_LOCK = new Object();

    public static String lastDecision = "IDLE";
    public static String pendingAction = "none";
    public static int pendingDuration = 0;
    public static String pendingReason = "No pending command";
}
