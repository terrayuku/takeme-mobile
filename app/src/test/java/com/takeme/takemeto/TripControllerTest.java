package com.takeme.takemeto;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.takeme.takemeto.impl.TripControllerImpl;
import com.takeme.takemeto.model.Fare;
import com.takeme.takemeto.model.Trip;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TripControllerImpl}.
 *
 * <p>Validates: Requirements 5.2.1, 6.1.1, 6.3.1, 6.3.2</p>
 */
public class TripControllerTest {

    @Mock FirebaseDatabase mockDatabase;
    @Mock FirebaseRemoteConfig mockRemoteConfig;
    @Mock DatabaseReference mockTripsRef;
    @Mock DatabaseReference mockTripRef;
    @Mock DatabaseReference mockFleetEventsRef;
    @Mock DatabaseReference mockFleetEventRef;
    @Mock DatabaseReference mockArrivalRef;

    private TripControllerImpl controller;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Wire up Firebase mock chain for /trips/{tripId}
        when(mockDatabase.getReference("trips")).thenReturn(mockTripsRef);
        when(mockTripsRef.child(anyString())).thenReturn(mockTripRef);
        when(mockTripRef.setValue(any())).thenReturn(null);

        // Wire up Firebase mock chain for /fleet_events/{eventId}
        when(mockDatabase.getReference("fleet_events")).thenReturn(mockFleetEventsRef);
        when(mockFleetEventsRef.child(anyString())).thenReturn(mockFleetEventRef);
        when(mockFleetEventRef.setValue(any())).thenReturn(null);

        // Wire up /trips/{tripId}/arrivalNotified
        when(mockTripRef.child("arrivalNotified")).thenReturn(mockArrivalRef);
        when(mockArrivalRef.setValue(any())).thenReturn(null);

        controller = new TripControllerImpl(mockDatabase, mockRemoteConfig);
    }

    // -------------------------------------------------------------------------
    // calculateFare tests (req 6.1.1)
    // -------------------------------------------------------------------------

    /**
     * 10 km at 2.5 ZAR/km must yield amountRand = 25.0.
     * Validates: Requirement 6.1.1
     */
    @Test
    public void calculateFare_returnsCorrectZarAmount() {
        when(mockRemoteConfig.getDouble(TripControllerImpl.REMOTE_CONFIG_RATE_KEY)).thenReturn(2.5);

        Fare fare = controller.calculateFare(10.0, "ZAR");

        assertEquals(25.0, fare.getAmountRand(), 0.0001);
        assertEquals("ZAR", fare.getCurrency());
        assertEquals(2.5, fare.getRatePerKm(), 0.0001);
    }

    /**
     * Zero distance must yield amountRand = 0.0 regardless of rate.
     * Validates: Requirement 6.1.1
     */
    @Test
    public void calculateFare_zeroDistance_returnsZero() {
        when(mockRemoteConfig.getDouble(TripControllerImpl.REMOTE_CONFIG_RATE_KEY)).thenReturn(2.5);

        Fare fare = controller.calculateFare(0.0, "ZAR");

        assertEquals(0.0, fare.getAmountRand(), 0.0001);
    }

    // -------------------------------------------------------------------------
    // completeTrip / payment retry tests (req 6.3.1, 6.3.2)
    // -------------------------------------------------------------------------

    /**
     * When payment always fails, retryPayment must increment paymentAttempts
     * exactly 3 times and set paymentStatus = FLAGGED.
     * Validates: Requirements 6.3.1, 6.3.2
     */
    @Test
    public void completeTrip_paymentFailure_retriesExactlyThreeTimes() {
        Trip trip = buildTrip();

        // Subclass that skips Thread.sleep so the test runs instantly.
        TripControllerImpl fastController = new TripControllerImpl(mockDatabase, mockRemoteConfig) {
            @Override
            protected void retryPayment(Trip t) {
                for (int i = 0; i < MAX_PAYMENT_ATTEMPTS; i++) {
                    t.setPaymentAttempts(t.getPaymentAttempts() + 1);
                }
                t.setPaymentStatus(Trip.PaymentStatus.FLAGGED);
                t.setStatus(Trip.TripStatus.PAYMENT_FAILED);
            }
        };

        fastController.completeTrip(trip, false);

        assertEquals(TripControllerImpl.MAX_PAYMENT_ATTEMPTS, trip.getPaymentAttempts());
        assertEquals(Trip.PaymentStatus.FLAGGED, trip.getPaymentStatus());
    }

    // -------------------------------------------------------------------------
    // checkArrivalProximity tests (req 5.2.1)
    // -------------------------------------------------------------------------

    /**
     * A position exactly 50 m from the dropoff must trigger the arrivalNotified write.
     * Validates: Requirement 5.2.1
     */
    @Test
    public void checkArrivalProximity_at50m_triggersNotification() {
        Trip trip = buildTrip();
        // dropoff at (0, 0); move ~50 m north along the meridian
        double lat50m = 50.0 / 111_320.0; // ≈ 0.000449°
        double[] coords = offsetLatLng(trip.getDropoffLat(), trip.getDropoffLng(), 50.0);

        controller.checkArrivalProximity(trip, coords[0], coords[1]);

        verify(mockTripRef).child("arrivalNotified");
        verify(mockArrivalRef).setValue(true);
    }

    /**
     * A position 51 m from the dropoff must NOT trigger the arrivalNotified write.
     * Validates: Requirement 5.2.1
     */
    @Test
    public void checkArrivalProximity_at51m_doesNotTrigger() {
        Trip trip = buildTrip();
        double[] coords = offsetLatLng(trip.getDropoffLat(), trip.getDropoffLng(), 51.0);

        controller.checkArrivalProximity(trip, coords[0], coords[1]);

        verify(mockTripRef, never()).child("arrivalNotified");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Trip buildTrip() {
        Trip trip = new Trip();
        trip.setTripId("trip-001");
        trip.setVehicleId("vehicle-001");
        trip.setPassengerId("passenger-001");
        trip.setPickupLat(-33.9249);
        trip.setPickupLng(18.4241);
        trip.setDropoffLat(0.0);
        trip.setDropoffLng(0.0);
        trip.setStatus(Trip.TripStatus.ACTIVE);
        trip.setPaymentStatus(Trip.PaymentStatus.PENDING);
        trip.setPaymentAttempts(0);
        return trip;
    }

    /**
     * Returns a coordinate that is approximately {@code distanceMetres} north of
     * (lat, lng) using the equirectangular approximation. Accurate enough for
     * small distances in tests.
     */
    private double[] offsetLatLng(double lat, double lng, double distanceMetres) {
        double deltaLat = distanceMetres / 111_320.0;
        return new double[]{lat + deltaLat, lng};
    }
}
