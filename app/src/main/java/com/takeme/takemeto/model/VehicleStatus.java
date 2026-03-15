package com.takeme.takemeto.model;

/**
 * Real-time vehicle status published to /fleet_status/{vehicleId} (req 7.1.1).
 * Flat GPS fields for Firebase RTDB compatibility.
 */
public class VehicleStatus {

    private String vehicleId;
    private double gpsLat;
    private double gpsLng;
    private double speedKmh;
    private Trip.TripStatus currentTripStatus;
    private boolean cameraHealthy;
    private boolean detectorAvailable;
    private boolean requiresInspection;
    private long timestampMs;

    /** No-arg constructor required for Firebase deserialization. */
    public VehicleStatus() {}

    public VehicleStatus(String vehicleId, double gpsLat, double gpsLng, double speedKmh,
                          Trip.TripStatus currentTripStatus, boolean cameraHealthy,
                          boolean detectorAvailable, boolean requiresInspection, long timestampMs) {
        this.vehicleId = vehicleId;
        this.gpsLat = gpsLat;
        this.gpsLng = gpsLng;
        this.speedKmh = speedKmh;
        this.currentTripStatus = currentTripStatus;
        this.cameraHealthy = cameraHealthy;
        this.detectorAvailable = detectorAvailable;
        this.requiresInspection = requiresInspection;
        this.timestampMs = timestampMs;
    }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public double getGpsLat() { return gpsLat; }
    public void setGpsLat(double gpsLat) { this.gpsLat = gpsLat; }

    public double getGpsLng() { return gpsLng; }
    public void setGpsLng(double gpsLng) { this.gpsLng = gpsLng; }

    public double getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(double speedKmh) { this.speedKmh = speedKmh; }

    public Trip.TripStatus getCurrentTripStatus() { return currentTripStatus; }
    public void setCurrentTripStatus(Trip.TripStatus currentTripStatus) { this.currentTripStatus = currentTripStatus; }

    public boolean isCameraHealthy() { return cameraHealthy; }
    public void setCameraHealthy(boolean cameraHealthy) { this.cameraHealthy = cameraHealthy; }

    public boolean isDetectorAvailable() { return detectorAvailable; }
    public void setDetectorAvailable(boolean detectorAvailable) { this.detectorAvailable = detectorAvailable; }

    public boolean isRequiresInspection() { return requiresInspection; }
    public void setRequiresInspection(boolean requiresInspection) { this.requiresInspection = requiresInspection; }

    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }
}
