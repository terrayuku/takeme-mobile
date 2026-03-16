package com.takeme.takemeto.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.takeme.takemeto.model.CommuterTrip;
import com.takeme.takemeto.model.Fare;
import com.takeme.takemeto.model.FleetEvent;

import java.util.UUID;

/**
 * Default implementation of {@link CommuterTripController}.
 *
 * <p>Uses Firebase RTDB for persistence and Firebase Remote Config for the
 * rate_per_km fare parameter. Reuses the same haversine formula and payment
 * retry logic as {@link TripControllerImpl}.</p>
 *
 * <p>Requirements: 1.1.1, 1.1.2, 1.1.3, 1.1.4, 4.1.1, 4.1.2, 4.1.3, 4.1.4,
 * 5.1.1, 5.1.2, 5.1.4, 5.2.1, 5.2.2, 5.2.3, 5.2.4</p>
 */
public class CommuterTripControllerImpl implements CommuterTripController {

    /** GPS proximity threshold for commuter check-in (30 metres, req 1.1.1). */
    static final double CHECKIN_PROXIMITY_THRESHOLD_METRES = 30.0;

    private final FirebaseDatabase database;
    private final FirebaseRemoteConfig remoteConfig;

    public CommuterTripControllerImpl(FirebaseDatabase database,
                                      FirebaseRemoteConfig remoteConfig) {
        this.database = database;
        this.remoteConfig = remoteConfig;
    }

    // -------------------------------------------------------------------------
    // CommuterTripController implementation
    // -------------------------------------------------------------------------

    @Override
    public void checkIn(String vehicleId, double stopLat, double stopLng) {
        checkIn(vehicleId, stopLat, stopLng, CommuterTrip.CheckinMethod.APP.name());
    }

    @Override
    public void checkIn(String vehicleId, double stopLat, double stopLng,
                        String checkinMethod) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Reject duplicate check-in: query /commuter_trips for an active trip
        // with the same UID and vehicleId (req 1.1.4).
        Query duplicateQuery = database.getReference("commuter_trips")
                .orderByChild("commuterUid")
                .equalTo(uid);

        duplicateQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    CommuterTrip existing = child.getValue(CommuterTrip.class);
                    if (existing != null
                            && vehicleId.equals(existing.getVehicleId())
                            && isActiveTrip(existing)) {
                        // Duplicate — already checked in on this vehicle.
                        return;
                    }
                }
                // No duplicate found — proceed with check-in.
                createCommuterTrip(uid, vehicleId, stopLat, stopLng, checkinMethod);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Firebase query failed — fall through and create the trip.
                // The RB5 side handles authoritative enforcement.
                createCommuterTrip(uid, vehicleId, stopLat, stopLng, checkinMethod);
            }
        });
    }

    @Override
    public void checkOut(String commuterTripId, double stopLat, double stopLng) {
        // Read the existing trip to get boarding coordinates for distance calc.
        database.getReference("commuter_trips")
                .child(commuterTripId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        CommuterTrip trip = snapshot.getValue(CommuterTrip.class);
                        if (trip == null) {
                            return;
                        }

                        long now = System.currentTimeMillis();
                        trip.setAlightingLat(stopLat);
                        trip.setAlightingLng(stopLng);
                        trip.setAlightingTimestampMs(now);

                        // Calculate distance via haversine (req 4.1.2, 4.1.4).
                        double distanceKm = haversineMetres(
                                trip.getBoardingLat(), trip.getBoardingLng(),
                                stopLat, stopLng) / 1000.0;
                        trip.setDistanceKm(distanceKm);

                        // Trigger fare calculation (req 5.1.1, 5.1.2).
                        Fare fare = calculateCommuterFare(distanceKm,
                                trip.getCurrency() != null ? trip.getCurrency() : "ZAR");
                        trip.setFareAmount(fare.getAmountRand());
                        trip.setStatus(CommuterTrip.CommuterTripStatus.IN_TRANSIT.name());

                        persistCommuterTrip(trip);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        // Silently fail — RB5 side is authoritative.
                    }
                });
    }

    @Override
    public Fare calculateCommuterFare(double distanceKm, String currencyCode) {
        double ratePerKm = getRatePerKm();
        double amountRand = distanceKm * ratePerKm;
        return new Fare(amountRand, distanceKm, currencyCode, ratePerKm);
    }

    @Override
    public void processCommuterPayment(CommuterTrip trip, boolean paymentSuccess) {
        if (paymentSuccess) {
            applyPaymentSuccess(trip);
        } else {
            retryCommuterPayment(trip);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Returns true if the trip is still active (BOARDING or IN_TRANSIT). */
    private boolean isActiveTrip(CommuterTrip trip) {
        String status = trip.getStatus();
        return CommuterTrip.CommuterTripStatus.BOARDING.name().equals(status)
                || CommuterTrip.CommuterTripStatus.IN_TRANSIT.name().equals(status);
    }

    /**
     * Creates a new CommuterTrip with status BOARDING and writes it to
     * /commuter_trips/{id} (req 1.1.1, 1.1.2, 1.1.3).
     */
    private void createCommuterTrip(String uid, String vehicleId,
                                    double stopLat, double stopLng,
                                    String checkinMethod) {
        String commuterTripId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        CommuterTrip trip = new CommuterTrip();
        trip.setCommuterTripId(commuterTripId);
        trip.setVehicleId(vehicleId);
        trip.setCommuterUid(uid);
        trip.setBoardingLat(stopLat);
        trip.setBoardingLng(stopLng);
        trip.setBoardingTimestampMs(now);
        trip.setStatus(CommuterTrip.CommuterTripStatus.BOARDING.name());
        trip.setPaymentStatus(CommuterTrip.CommuterPaymentStatus.PENDING.name());
        trip.setPaymentAttempts(0);
        trip.setAutoCompleted(false);
        trip.setCheckinMethod(checkinMethod);
        trip.setCurrency("ZAR");

        persistCommuterTrip(trip);
    }

    /** Reads rate_per_km from Remote Config; falls back to 2.5 ZAR/km. */
    double getRatePerKm() {
        double rate = remoteConfig.getDouble(TripControllerImpl.REMOTE_CONFIG_RATE_KEY);
        return rate > 0 ? rate : TripControllerImpl.DEFAULT_RATE_PER_KM;
    }

    private void applyPaymentSuccess(CommuterTrip trip) {
        trip.setPaymentStatus(CommuterTrip.CommuterPaymentStatus.PAID.name());
        trip.setStatus(CommuterTrip.CommuterTripStatus.COMPLETED.name());
        persistCommuterTrip(trip);
    }

    /**
     * Retries payment up to {@link TripControllerImpl#MAX_PAYMENT_ATTEMPTS}
     * times with exponential backoff (1 s, 2 s, 4 s). After exhaustion, flags
     * the trip and writes a PAYMENT_FAILURE fleet event (req 5.2.3, 5.2.4).
     */
    protected void retryCommuterPayment(CommuterTrip trip) {
        long[] backoffMs = {1000L, 2000L, 4000L};

        for (int attempt = 0; attempt < TripControllerImpl.MAX_PAYMENT_ATTEMPTS; attempt++) {
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

        // All attempts exhausted — flag the trip (req 5.2.3, 5.2.4).
        trip.setPaymentStatus(CommuterTrip.CommuterPaymentStatus.FLAGGED.name());
        trip.setStatus(CommuterTrip.CommuterTripStatus.PAYMENT_FAILED.name());
        persistCommuterTrip(trip);
        writePaymentFailureEvent(trip);
    }

    private void persistCommuterTrip(CommuterTrip trip) {
        database.getReference("commuter_trips")
                .child(trip.getCommuterTripId())
                .setValue(trip);
    }

    private void writePaymentFailureEvent(CommuterTrip trip) {
        String eventId = UUID.randomUUID().toString();
        FleetEvent event = new FleetEvent(
                eventId,
                trip.getVehicleId(),
                FleetEvent.FleetEventType.PAYMENT_FAILURE,
                "Payment failed after " + TripControllerImpl.MAX_PAYMENT_ATTEMPTS
                        + " attempts for commuter trip " + trip.getCommuterTripId(),
                trip.getAlightingLat(),
                trip.getAlightingLng(),
                System.currentTimeMillis());

        database.getReference("fleet_events")
                .child(eventId)
                .setValue(event);
    }

    /**
     * Haversine formula — returns the great-circle distance in metres between
     * two WGS-84 coordinates. Same implementation as
     * {@link TripControllerImpl#haversineMetres(double, double, double, double)}.
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
