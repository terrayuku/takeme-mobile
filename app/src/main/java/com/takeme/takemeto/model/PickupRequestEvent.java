package com.takeme.takemeto.model;

/**
 * Represents a pickup request event emitted when a hand signal is detected
 * with confidence >= 0.75 (req 2.2.1, 2.2.2).
 * Flat GPS fields for Firebase RTDB compatibility (req 4.1, 4.2).
 */
public class PickupRequestEvent {

    public enum PickupRequestStatus {
        PENDING, CONFIRMED, CANCELLED, UNCONFIRMED
    }

    private String requestId;
    private String vehicleId;
    private double gpsLat;
    private double gpsLng;
    private long timestampMs;
    private float confidenceScore;
    private PickupRequestStatus status;

    /** No-arg constructor required for Firebase deserialization. */
    public PickupRequestEvent() {}

    public PickupRequestEvent(String requestId, String vehicleId, double gpsLat, double gpsLng,
                               long timestampMs, float confidenceScore, PickupRequestStatus status) {
        this.requestId = requestId;
        this.vehicleId = vehicleId;
        this.gpsLat = gpsLat;
        this.gpsLng = gpsLng;
        this.timestampMs = timestampMs;
        this.confidenceScore = confidenceScore;
        this.status = status;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public double getGpsLat() { return gpsLat; }
    public void setGpsLat(double gpsLat) { this.gpsLat = gpsLat; }

    public double getGpsLng() { return gpsLng; }
    public void setGpsLng(double gpsLng) { this.gpsLng = gpsLng; }

    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }

    public float getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(float confidenceScore) { this.confidenceScore = confidenceScore; }

    public PickupRequestStatus getStatus() { return status; }
    public void setStatus(PickupRequestStatus status) { this.status = status; }
}
