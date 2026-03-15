package com.takeme.takemeto.model;

/**
 * Represents a passenger trip (req 4.2.2, 6.1, 6.2, 6.3, 6.4).
 * Flat GPS fields for Firebase RTDB compatibility.
 */
public class Trip {

    public enum TripStatus {
        ACTIVE, COMPLETED, PAYMENT_FAILED
    }

    public enum PaymentStatus {
        PENDING, PAID, FAILED, FLAGGED
    }

    private String tripId;
    private String vehicleId;
    private String passengerId;
    private double pickupLat;
    private double pickupLng;
    private double dropoffLat;
    private double dropoffLng;
    private long pickupTimestampMs;
    private long dropoffTimestampMs;
    private double distanceKm;
    private Fare fare;
    private TripStatus status;
    private PaymentStatus paymentStatus;
    private int paymentAttempts;

    /** No-arg constructor required for Firebase deserialization. */
    public Trip() {}

    public Trip(String tripId, String vehicleId, String passengerId,
                double pickupLat, double pickupLng,
                double dropoffLat, double dropoffLng,
                long pickupTimestampMs, long dropoffTimestampMs,
                double distanceKm, Fare fare,
                TripStatus status, PaymentStatus paymentStatus, int paymentAttempts) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.passengerId = passengerId;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
        this.pickupTimestampMs = pickupTimestampMs;
        this.dropoffTimestampMs = dropoffTimestampMs;
        this.distanceKm = distanceKm;
        this.fare = fare;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.paymentAttempts = paymentAttempts;
    }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }

    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }

    public double getDropoffLat() { return dropoffLat; }
    public void setDropoffLat(double dropoffLat) { this.dropoffLat = dropoffLat; }

    public double getDropoffLng() { return dropoffLng; }
    public void setDropoffLng(double dropoffLng) { this.dropoffLng = dropoffLng; }

    public long getPickupTimestampMs() { return pickupTimestampMs; }
    public void setPickupTimestampMs(long pickupTimestampMs) { this.pickupTimestampMs = pickupTimestampMs; }

    public long getDropoffTimestampMs() { return dropoffTimestampMs; }
    public void setDropoffTimestampMs(long dropoffTimestampMs) { this.dropoffTimestampMs = dropoffTimestampMs; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public Fare getFare() { return fare; }
    public void setFare(Fare fare) { this.fare = fare; }

    public TripStatus getStatus() { return status; }
    public void setStatus(TripStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public int getPaymentAttempts() { return paymentAttempts; }
    public void setPaymentAttempts(int paymentAttempts) { this.paymentAttempts = paymentAttempts; }
}
