package com.takeme.takemeto.impl;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.takeme.takemeto.model.Fare;
import com.takeme.takemeto.model.FleetEvent;
import com.takeme.takemeto.model.Trip;

import java.util.UUID;

/**
 * Default implementation of {@link TripController}.
 *
 * <p>Uses Firebase RTDB for persistence and Firebase Remote Config for the
 * rate_per_km fare parameter. Payment retry uses exponential backoff
 * (1 s, 2 s, 4 s) for up to 3 attempts (req 6.3.1).</p>
 */
public class TripControllerImpl implements TripController {

    public static final String REMOTE_CONFIG_RATE_KEY = "rate_per_km";
    public static final double DEFAULT_RATE_PER_KM = 2.5;
    public static final int MAX_PAYMENT_ATTEMPTS = 3;
    static final double ARRIVAL_THRESHOLD_METRES = 50.0;

    private final FirebaseDatabase database;
    private final FirebaseRemoteConfig remoteConfig;

    public TripControllerImpl(FirebaseDatabase database, FirebaseRemoteConfig remoteConfig) {
        this.database = database;
        this.remoteConfig = remoteConfig;
    }

    // -------------------------------------------------------------------------
    // TripController implementation
    // -------------------------------------------------------------------------

    @Override
    public void startTrip(Trip trip) {
        database.getReference("trips")
                .child(trip.getTripId())
                .setValue(trip);
    }

    @Override
    public void onDestinationSelected(Trip trip, double destLat, double destLng) {
        trip.setDropoffLat(destLat);
        trip.setDropoffLng(destLng);
        // Route calculation via Directions API would populate distanceKm here.
        // For now we persist the updated dropoff coordinates.
        database.getReference("trips")
                .child(trip.getTripId())
                .setValue(trip);
    }

    @Override
    public Fare calculateFare(double distanceKm, String currencyCode) {
        double ratePerKm = getRatePerKm();
        double amountRand = distanceKm * ratePerKm;
        return new Fare(amountRand, distanceKm, currencyCode, ratePerKm);
    }

    @Override
    public void completeTrip(Trip trip, boolean paymentSuccess) {
        if (paymentSuccess) {
            applyPaymentSuccess(trip);
        } else {
            retryPayment(trip);
        }
    }

    @Override
    public void checkArrivalProximity(Trip trip, double currentLat, double currentLng) {
        double distanceMetres = haversineMetres(
                currentLat, currentLng,
                trip.getDropoffLat(), trip.getDropoffLng());

        if (distanceMetres <= ARRIVAL_THRESHOLD_METRES) {
            database.getReference("trips")
                    .child(trip.getTripId())
                    .child("arrivalNotified")
                    .setValue(true);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Reads rate_per_km from Remote Config; falls back to 2.5 ZAR/km. */
    double getRatePerKm() {
        double rate = remoteConfig.getDouble(REMOTE_CONFIG_RATE_KEY);
        return rate > 0 ? rate : DEFAULT_RATE_PER_KM;
    }

    private void applyPaymentSuccess(Trip trip) {
        trip.setPaymentStatus(Trip.PaymentStatus.PAID);
        trip.setStatus(Trip.TripStatus.COMPLETED);
        persistTrip(trip);
    }

    /**
     * Retries payment up to {@link #MAX_PAYMENT_ATTEMPTS} times with
     * exponential backoff (1 s, 2 s, 4 s). After exhaustion, flags the trip
     * and writes a PAYMENT_FAILURE fleet event (req 6.3.1, 6.3.2).
     */
    protected void retryPayment(Trip trip) {
        long[] backoffMs = {1000L, 2000L, 4000L};

        for (int attempt = 0; attempt < MAX_PAYMENT_ATTEMPTS; attempt++) {
            trip.setPaymentAttempts(trip.getPaymentAttempts() + 1);

            // Simulate the backoff delay (in production this would be async).
            try {
                Thread.sleep(backoffMs[attempt]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // In a real implementation the Peach Payments Cloud Function would
            // be called here. We treat each iteration as a failed attempt.
        }

        // All attempts exhausted — flag the trip.
        trip.setPaymentStatus(Trip.PaymentStatus.FLAGGED);
        trip.setStatus(Trip.TripStatus.PAYMENT_FAILED);
        persistTrip(trip);
        writePaymentFailureEvent(trip);
    }

    private void persistTrip(Trip trip) {
        database.getReference("trips")
                .child(trip.getTripId())
                .setValue(trip);
    }

    private void writePaymentFailureEvent(Trip trip) {
        String eventId = UUID.randomUUID().toString();
        FleetEvent event = new FleetEvent(
                eventId,
                trip.getVehicleId(),
                FleetEvent.FleetEventType.PAYMENT_FAILURE,
                "Payment failed after " + MAX_PAYMENT_ATTEMPTS + " attempts for trip " + trip.getTripId(),
                trip.getDropoffLat(),
                trip.getDropoffLng(),
                System.currentTimeMillis());

        database.getReference("fleet_events")
                .child(eventId)
                .setValue(event);
    }

    /**
     * Haversine formula — returns the great-circle distance in metres between
     * two WGS-84 coordinates.
     */
    static double haversineMetres(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6_371_000.0; // Earth radius in metres
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
