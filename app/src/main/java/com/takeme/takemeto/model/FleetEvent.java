package com.takeme.takemeto.model;

/**
 * Fleet event written to /fleet_events/{eventId}, retained 30 days (req 7.4.1).
 * Flat GPS fields for Firebase RTDB compatibility.
 */
public class FleetEvent {

    public enum FleetEventType {
        CAMERA_FAILURE,
        PICKUP_CANCELLED,
        OBSTACLE_ABORT,
        FP_RATE_ALERT,
        PAYMENT_FAILURE,
        FPS_WARNING,
        INFERENCE_ERROR,
        TRIP_CREATE_ERROR
    }

    private String eventId;
    private String vehicleId;
    private FleetEventType eventType;
    private String detail;
    private double gpsLat;
    private double gpsLng;
    private long timestampMs;

    /** No-arg constructor required for Firebase deserialization. */
    public FleetEvent() {}

    public FleetEvent(String eventId, String vehicleId, FleetEventType eventType,
                      String detail, double gpsLat, double gpsLng, long timestampMs) {
        this.eventId = eventId;
        this.vehicleId = vehicleId;
        this.eventType = eventType;
        this.detail = detail;
        this.gpsLat = gpsLat;
        this.gpsLng = gpsLng;
        this.timestampMs = timestampMs;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public FleetEventType getEventType() { return eventType; }
    public void setEventType(FleetEventType eventType) { this.eventType = eventType; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public double getGpsLat() { return gpsLat; }
    public void setGpsLat(double gpsLat) { this.gpsLat = gpsLat; }

    public double getGpsLng() { return gpsLng; }
    public void setGpsLng(double gpsLng) { this.gpsLng = gpsLng; }

    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }
}
