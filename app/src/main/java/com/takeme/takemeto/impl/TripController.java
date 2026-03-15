package com.takeme.takemeto.impl;

import com.takeme.takemeto.model.Fare;
import com.takeme.takemeto.model.Trip;

/**
 * Controls the lifecycle of a passenger trip: start, destination selection,
 * fare calculation, payment, and arrival proximity (req 5.1.2, 5.2.1, 6.1.1,
 * 6.2.1, 6.2.2, 6.3.1, 6.3.2, 6.4.1, 8.4).
 */
public interface TripController {

    /**
     * Writes the trip record to Firebase RTDB at /trips/{tripId}.
     */
    void startTrip(Trip trip);

    /**
     * Calculates the optimal route via Google Directions API and updates the
     * trip record with dropoff coordinates and distance (req 5.1.2).
     */
    void onDestinationSelected(Trip trip, double destLat, double destLng);

    /**
     * Calculates the fare: amountRand = distanceKm * ratePerKm.
     * ratePerKm is sourced from Firebase Remote Config key "rate_per_km"
     * (default 2.5 ZAR/km). Currency is jurisdiction-aware (req 6.1.1, 8.4).
     */
    Fare calculateFare(double distanceKm, String currencyCode);

    /**
     * Completes the trip. On success: sets paymentStatus=PAID, status=COMPLETED,
     * writes to RTDB. On failure: retries up to 3 times with exponential backoff
     * (1s, 2s, 4s); after 3 failures sets paymentStatus=FLAGGED and writes a
     * FleetEvent(PAYMENT_FAILURE) (req 6.2.1, 6.2.2, 6.3.1, 6.3.2).
     */
    void completeTrip(Trip trip, boolean paymentSuccess);

    /**
     * Checks whether the vehicle is within 50 metres of the trip's dropoff
     * coordinates. If so, writes arrivalNotified=true to
     * /trips/{tripId}/arrivalNotified (req 5.2.1).
     */
    void checkArrivalProximity(Trip trip, double currentLat, double currentLng);
}
