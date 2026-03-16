package com.takeme.takemeto.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a stop event grouping all boarding and alighting actions at a single stop.
 * Contains stop GPS coordinates, timestamp, lists of commuter check-ins and check-outs,
 * sensor counts, reconciliation status, and enforcement action taken.
 * Flat GPS fields for Firebase RTDB compatibility.
 *
 * Requirements: 1.3.4, 6.2.1
 */
public class StopEvent {

    public enum ReconciliationStatus {
        RECONCILED, DISCREPANCY_DETECTED
    }

    public enum EnforcementAction {
        NONE, PRIMARY_HOLD, GRACE_PERIOD, SAFE_STOP_ROUTE
    }

    private String stopEventId;
    private String vehicleId;
    private double stopLat;
    private double stopLng;
    private long timestampMs;
    private List<String> checkinIds;
    private List<String> checkoutIds;
    private int seatCount;
    private int cameraHeadcount;
    private int checkinCount;
    private String reconciliationStatus;
    private String enforcementAction;

    /** No-arg constructor required for Firebase deserialization. */
    public StopEvent() {}

    public StopEvent(String stopEventId, String vehicleId, double stopLat, double stopLng,
                     long timestampMs, List<String> checkinIds, List<String> checkoutIds,
                     int seatCount, int cameraHeadcount, int checkinCount,
                     String reconciliationStatus, String enforcementAction) {
        this.stopEventId = stopEventId;
        this.vehicleId = vehicleId;
        this.stopLat = stopLat;
        this.stopLng = stopLng;
        this.timestampMs = timestampMs;
        this.checkinIds = checkinIds != null ? checkinIds : new ArrayList<>();
        this.checkoutIds = checkoutIds != null ? checkoutIds : new ArrayList<>();
        this.seatCount = seatCount;
        this.cameraHeadcount = cameraHeadcount;
        this.checkinCount = checkinCount;
        this.reconciliationStatus = reconciliationStatus;
        this.enforcementAction = enforcementAction;
    }

    public String getStopEventId() { return stopEventId; }
    public void setStopEventId(String stopEventId) { this.stopEventId = stopEventId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public double getStopLat() { return stopLat; }
    public void setStopLat(double stopLat) { this.stopLat = stopLat; }

    public double getStopLng() { return stopLng; }
    public void setStopLng(double stopLng) { this.stopLng = stopLng; }

    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }

    public List<String> getCheckinIds() { return checkinIds; }
    public void setCheckinIds(List<String> checkinIds) { this.checkinIds = checkinIds; }

    public List<String> getCheckoutIds() { return checkoutIds; }
    public void setCheckoutIds(List<String> checkoutIds) { this.checkoutIds = checkoutIds; }

    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }

    public int getCameraHeadcount() { return cameraHeadcount; }
    public void setCameraHeadcount(int cameraHeadcount) { this.cameraHeadcount = cameraHeadcount; }

    public int getCheckinCount() { return checkinCount; }
    public void setCheckinCount(int checkinCount) { this.checkinCount = checkinCount; }

    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }

    public String getEnforcementAction() { return enforcementAction; }
    public void setEnforcementAction(String enforcementAction) { this.enforcementAction = enforcementAction; }
}
