package com.takeme.takemeto.model;

/**
 * Anonymised detection log entry — no PII, no image data (req 8.1, 8.2, 8.3, 10.1).
 * Written to /detection_logs/{detectionId}.
 */
public class DetectionLogEntry {

    public enum DetectionOutcome {
        CONFIRMED_PICKUP, UNCONFIRMED, FALSE_POSITIVE
    }

    private String detectionId;
    private String vehicleId;
    private float confidenceScore;
    private DetectionOutcome outcome;
    private int hourOfDay;
    private String lightingEstimate;
    private long timestampMs;

    /** No-arg constructor required for Firebase deserialization. */
    public DetectionLogEntry() {}

    public DetectionLogEntry(String detectionId, String vehicleId, float confidenceScore,
                              DetectionOutcome outcome, int hourOfDay,
                              String lightingEstimate, long timestampMs) {
        this.detectionId = detectionId;
        this.vehicleId = vehicleId;
        this.confidenceScore = confidenceScore;
        this.outcome = outcome;
        this.hourOfDay = hourOfDay;
        this.lightingEstimate = lightingEstimate;
        this.timestampMs = timestampMs;
    }

    public String getDetectionId() { return detectionId; }
    public void setDetectionId(String detectionId) { this.detectionId = detectionId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public float getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(float confidenceScore) { this.confidenceScore = confidenceScore; }

    public DetectionOutcome getOutcome() { return outcome; }
    public void setOutcome(DetectionOutcome outcome) { this.outcome = outcome; }

    public int getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(int hourOfDay) { this.hourOfDay = hourOfDay; }

    public String getLightingEstimate() { return lightingEstimate; }
    public void setLightingEstimate(String lightingEstimate) { this.lightingEstimate = lightingEstimate; }

    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }
}
