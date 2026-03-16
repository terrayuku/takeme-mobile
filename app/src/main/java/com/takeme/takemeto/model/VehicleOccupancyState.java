package com.takeme.takemeto.model;

/**
 * Represents the real-time occupancy state of a vehicle.
 * Published to /vehicle_occupancy/{vehicleId} at ≤5s intervals.
 *
 * Requirements: 6.1.2
 */
public class VehicleOccupancyState {

    private String vehicleId;
    private int seatSensorCount;
    private int cameraHeadcount;
    private int checkedInCount;
    private int activeCommuterTrips;
    private int maxCapacity;
    private boolean boardingWindowOpen;
    private boolean alightingWindowOpen;
    private int gracePeriodStopsRemaining;
    private long timestampMs;

    /** No-arg constructor required for Firebase deserialization. */
    public VehicleOccupancyState() {}

    public VehicleOccupancyState(String vehicleId, int seatSensorCount, int cameraHeadcount,
                                 int checkedInCount, int activeCommuterTrips, int maxCapacity,
                                 boolean boardingWindowOpen, boolean alightingWindowOpen,
                                 int gracePeriodStopsRemaining, long timestampMs) {
        this.vehicleId = vehicleId;
        this.seatSensorCount = seatSensorCount;
        this.cameraHeadcount = cameraHeadcount;
        this.checkedInCount = checkedInCount;
        this.activeCommuterTrips = activeCommuterTrips;
        this.maxCapacity = maxCapacity;
        this.boardingWindowOpen = boardingWindowOpen;
        this.alightingWindowOpen = alightingWindowOpen;
        this.gracePeriodStopsRemaining = gracePeriodStopsRemaining;
        this.timestampMs = timestampMs;
    }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public int getSeatSensorCount() { return seatSensorCount; }
    public void setSeatSensorCount(int seatSensorCount) { this.seatSensorCount = seatSensorCount; }

    public int getCameraHeadcount() { return cameraHeadcount; }
    public void setCameraHeadcount(int cameraHeadcount) { this.cameraHeadcount = cameraHeadcount; }

    public int getCheckedInCount() { return checkedInCount; }
    public void setCheckedInCount(int checkedInCount) { this.checkedInCount = checkedInCount; }

    public int getActiveCommuterTrips() { return activeCommuterTrips; }
    public void setActiveCommuterTrips(int activeCommuterTrips) { this.activeCommuterTrips = activeCommuterTrips; }

    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }

    public boolean isBoardingWindowOpen() { return boardingWindowOpen; }
    public void setBoardingWindowOpen(boolean boardingWindowOpen) { this.boardingWindowOpen = boardingWindowOpen; }

    public boolean isAlightingWindowOpen() { return alightingWindowOpen; }
    public void setAlightingWindowOpen(boolean alightingWindowOpen) { this.alightingWindowOpen = alightingWindowOpen; }

    public int getGracePeriodStopsRemaining() { return gracePeriodStopsRemaining; }
    public void setGracePeriodStopsRemaining(int gracePeriodStopsRemaining) { this.gracePeriodStopsRemaining = gracePeriodStopsRemaining; }

    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }
}
