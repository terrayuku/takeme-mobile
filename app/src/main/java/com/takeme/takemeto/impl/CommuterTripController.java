package com.takeme.takemeto.impl;

import com.takeme.takemeto.model.CommuterTrip;
import com.takeme.takemeto.model.Fare;

/**
 * Controls the lifecycle of a per-commuter trip on a multi-commuter vehicle:
 * check-in, check-out, fare calculation, and payment processing
 * (req 1.1.1, 1.1.2, 4.1.1, 4.1.2, 5.1.1, 5.2.1).
 */
public interface CommuterTripController {

    /**
     * Registers check-in by writing to /commuter_trips/{id} with status BOARDING.
     * Validates GPS proximity (≤30m from vehicle).
     * Rejects duplicate check-ins for same vehicle (req 1.1.1, 1.1.2).
     * Uses "APP" as the default checkinMethod.
     */
    void checkIn(String vehicleId, double stopLat, double stopLng);

    /**
     * Registers check-in with an explicit checkinMethod ("APP" or "QR_CODE").
     * Validates GPS proximity (≤30m from vehicle).
     * Rejects duplicate check-ins for same vehicle (req 1.1.1, 1.2.2, 1.2.3).
     */
    void checkIn(String vehicleId, double stopLat, double stopLng, String checkinMethod);

    /**
     * Registers check-out by updating /commuter_trips/{id} with alighting
     * coordinates and timestamp. Triggers fare calculation (req 4.1.1, 4.1.2).
     */
    void checkOut(String commuterTripId, double stopLat, double stopLng);

    /**
     * Calculates per-commuter fare: amountRand = distanceKm * ratePerKm.
     * ratePerKm is sourced from Firebase Remote Config key "rate_per_km"
     * (default 2.5 ZAR/km). Currency is jurisdiction-aware (req 5.1.1).
     */
    Fare calculateCommuterFare(double distanceKm, String currencyCode);

    /**
     * Processes payment for a CommuterTrip via Peach Payments.
     * On success: sets paymentStatus=PAID, status=COMPLETED, writes to RTDB.
     * On failure: retries up to 3 times with exponential backoff (1s, 2s, 4s);
     * after 3 failures sets paymentStatus=FLAGGED, status=PAYMENT_FAILED,
     * and publishes a PAYMENT_FAILURE fleet event (req 5.2.1).
     */
    void processCommuterPayment(CommuterTrip trip, boolean paymentSuccess);
}
