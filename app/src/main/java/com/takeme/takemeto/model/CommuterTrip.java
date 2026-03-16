package com.takeme.takemeto.model;

/**
 * Represents a per-commuter trip record for multi-commuter boarding.
 * Tracks an individual commuter's boarding stop, alighting stop, distance,
 * fare, payment status, and auto-completion flag.
 * Flat GPS fields for Firebase RTDB compatibility.
 *
 * Requirements: 1.1.2, 4.1.2, 5.1.1, 6.3.1
 */
public class CommuterTrip {

    public enum CommuterTripStatus {
        BOARDING, IN_TRANSIT, COMPLETED, PAYMENT_FAILED
    }

    public enum CommuterPaymentStatus {
        PENDING, PAID, FAILED, FLAGGED
    }

    public enum CheckinMethod {
        APP, QR_CODE
    }

    private String commuterTripId;
    private String vehicleId;
    private String commuterUid;
    private double boardingLat;
    private double boardingLng;
    private long boardingTimestampMs;
    private double alightingLat;
    private double alightingLng;
    private long alightingTimestampMs;
    private double distanceKm;
    private double fareAmount;
    private String currency;
    private String status;
    private String paymentStatus;
    private int paymentAttempts;
    private boolean autoCompleted;
    private String checkinMethod;
    private String stopEventId;

    /** No-arg constructor required for Firebase deserialization. */
    public CommuterTrip() {}

    public CommuterTrip(String commuterTripId, String vehicleId, String commuterUid,
                        double boardingLat, double boardingLng, long boardingTimestampMs,
                        double alightingLat, double alightingLng, long alightingTimestampMs,
                        double distanceKm, double fareAmount, String currency,
                        String status, String paymentStatus, int paymentAttempts,
                        boolean autoCompleted, String checkinMethod, String stopEventId) {
        this.commuterTripId = commuterTripId;
        this.vehicleId = vehicleId;
        this.commuterUid = commuterUid;
        this.boardingLat = boardingLat;
        this.boardingLng = boardingLng;
        this.boardingTimestampMs = boardingTimestampMs;
        this.alightingLat = alightingLat;
        this.alightingLng = alightingLng;
        this.alightingTimestampMs = alightingTimestampMs;
        this.distanceKm = distanceKm;
        this.fareAmount = fareAmount;
        this.currency = currency;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.paymentAttempts = paymentAttempts;
        this.autoCompleted = autoCompleted;
        this.checkinMethod = checkinMethod;
        this.stopEventId = stopEventId;
    }

    public String getCommuterTripId() { return commuterTripId; }
    public void setCommuterTripId(String commuterTripId) { this.commuterTripId = commuterTripId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getCommuterUid() { return commuterUid; }
    public void setCommuterUid(String commuterUid) { this.commuterUid = commuterUid; }

    public double getBoardingLat() { return boardingLat; }
    public void setBoardingLat(double boardingLat) { this.boardingLat = boardingLat; }

    public double getBoardingLng() { return boardingLng; }
    public void setBoardingLng(double boardingLng) { this.boardingLng = boardingLng; }

    public long getBoardingTimestampMs() { return boardingTimestampMs; }
    public void setBoardingTimestampMs(long boardingTimestampMs) { this.boardingTimestampMs = boardingTimestampMs; }

    public double getAlightingLat() { return alightingLat; }
    public void setAlightingLat(double alightingLat) { this.alightingLat = alightingLat; }

    public double getAlightingLng() { return alightingLng; }
    public void setAlightingLng(double alightingLng) { this.alightingLng = alightingLng; }

    public long getAlightingTimestampMs() { return alightingTimestampMs; }
    public void setAlightingTimestampMs(long alightingTimestampMs) { this.alightingTimestampMs = alightingTimestampMs; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public double getFareAmount() { return fareAmount; }
    public void setFareAmount(double fareAmount) { this.fareAmount = fareAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public int getPaymentAttempts() { return paymentAttempts; }
    public void setPaymentAttempts(int paymentAttempts) { this.paymentAttempts = paymentAttempts; }

    public boolean isAutoCompleted() { return autoCompleted; }
    public void setAutoCompleted(boolean autoCompleted) { this.autoCompleted = autoCompleted; }

    public String getCheckinMethod() { return checkinMethod; }
    public void setCheckinMethod(String checkinMethod) { this.checkinMethod = checkinMethod; }

    public String getStopEventId() { return stopEventId; }
    public void setStopEventId(String stopEventId) { this.stopEventId = stopEventId; }
}
