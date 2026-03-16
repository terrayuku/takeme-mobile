package com.takeme.takemeto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.takeme.takemeto.impl.CommuterTripController;
import com.takeme.takemeto.impl.CommuterTripControllerImpl;
import com.takeme.takemeto.model.CommuterTrip;
import com.takeme.takemeto.model.Fare;
import com.takeme.takemeto.model.VehicleOccupancyState;

import java.util.Locale;

/**
 * Commuter check-out screen for alighting from a multi-commuter vehicle.
 *
 * <p>Listens on {@code /vehicle_occupancy/{vehicleId}} for the
 * {@code alightingWindowOpen} state. When the commuter taps "Alight", the
 * activity records the check-out via {@link CommuterTripController#checkOut},
 * displays the calculated fare, and processes payment through Peach Payments.</p>
 *
 * <p>Handles auto-completed trips (where the RB5 completed the trip because the
 * commuter exited without checking out) by showing an informational message.</p>
 *
 * <p>Payment retry UI follows the same pattern as {@link PaymentActivity}.</p>
 *
 * <p>Requirements: 4.1.1, 4.1.2, 4.1.3, 4.1.4, 5.1.1, 5.1.3, 5.2.1, 5.2.2, 5.3.2</p>
 */
public class CommuterCheckOutActivity extends AppCompatActivity {

    public static final String EXTRA_COMMUTER_TRIP_ID = "commuterTripId";
    public static final String EXTRA_VEHICLE_ID = "vehicleId";

    static final int MAX_PAYMENT_ATTEMPTS = 3;
    private static final int LOCATION_PERMISSION_REQUEST = 2001;

    private String commuterTripId;
    private String vehicleId;

    private DatabaseReference occupancyRef;
    private ValueEventListener occupancyListener;
    private DatabaseReference tripRef;
    private ValueEventListener tripListener;
    private FusedLocationProviderClient fusedLocationClient;
    private CommuterTripController commuterTripController;

    private MaterialButton btnAlight;
    private TextView tvFareAmount;
    private TextView tvFareLabel;
    private TextView tvStatus;
    private MaterialButton btnPay;
    private ProgressBar progressBar;
    private MaterialCardView fareCard;

    private CommuterTrip currentTrip;
    private Fare currentFare;
    private int paymentAttempts = 0;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commuter_check_out);

        commuterTripId = getIntent().getStringExtra(EXTRA_COMMUTER_TRIP_ID);
        vehicleId = getIntent().getStringExtra(EXTRA_VEHICLE_ID);

        bindViews();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        commuterTripController = new CommuterTripControllerImpl(
                FirebaseDatabase.getInstance(),
                FirebaseRemoteConfig.getInstance());

        // Listen on /vehicle_occupancy/{vehicleId} for alighting window state (req 4.1.1)
        occupancyRef = FirebaseDatabase.getInstance()
                .getReference("vehicle_occupancy")
                .child(vehicleId);

        occupancyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                VehicleOccupancyState occupancy = snapshot.getValue(VehicleOccupancyState.class);
                updateUiFromOccupancy(occupancy);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silently handle — UI remains in last known state
            }
        };
        occupancyRef.addValueEventListener(occupancyListener);

        // Listen on /commuter_trips/{commuterTripId} for auto-completion (req 5.3.2)
        tripRef = FirebaseDatabase.getInstance()
                .getReference("commuter_trips")
                .child(commuterTripId);

        tripListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                CommuterTrip trip = snapshot.getValue(CommuterTrip.class);
                if (trip != null) {
                    currentTrip = trip;
                    handleTripUpdate(trip);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silently handle
            }
        };
        tripRef.addValueEventListener(tripListener);

        // "Alight" button — GPS check-out (req 4.1.1)
        btnAlight.setOnClickListener(v -> onAlightTapped());

        // "Pay" button — trigger payment (req 5.2.1)
        btnPay.setOnClickListener(v -> onPayTapped());
    }

    // -------------------------------------------------------------------------
    // Occupancy state handling
    // -------------------------------------------------------------------------

    /**
     * Updates UI based on the latest occupancy state from Firebase.
     * Enables the "Alight" button only when the alighting window is open.
     */
    private void updateUiFromOccupancy(VehicleOccupancyState occupancy) {
        if (occupancy == null) {
            return;
        }

        if (!occupancy.isAlightingWindowOpen()) {
            btnAlight.setEnabled(false);
            // Only update status if we haven't already checked out
            if (currentFare == null) {
                tvStatus.setText("Waiting for the vehicle to stop…");
                tvStatus.setVisibility(View.VISIBLE);
            }
        } else {
            btnAlight.setEnabled(true);
            tvStatus.setText("Vehicle is stopped. Tap Alight to check out.");
            tvStatus.setVisibility(View.VISIBLE);
        }
    }

    // -------------------------------------------------------------------------
    // Trip update handling (auto-completion)
    // -------------------------------------------------------------------------

    /**
     * Handles real-time trip updates from Firebase. If the trip was
     * auto-completed by the RB5 (commuter exited without checking out),
     * shows an informational message and presents the fare (req 5.3.2).
     */
    private void handleTripUpdate(CommuterTrip trip) {
        if (trip.isAutoCompleted() && trip.getFareAmount() > 0) {
            // Trip was auto-completed by the RB5 (req 5.3.2)
            btnAlight.setVisibility(View.GONE);

            tvStatus.setText("Your trip was auto-completed");
            tvStatus.setVisibility(View.VISIBLE);

            presentFare(trip);
        }
    }

    // -------------------------------------------------------------------------
    // Alight (check-out) flow
    // -------------------------------------------------------------------------

    /**
     * Called when the commuter taps "Alight". Gets current GPS location
     * and calls {@link CommuterTripController#checkOut} (req 4.1.1, 4.1.2).
     */
    private void onAlightTapped() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnAlight.setEnabled(false);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            progressBar.setVisibility(View.GONE);

            if (location == null) {
                tvStatus.setText("Unable to get your location. Please try again.");
                tvStatus.setVisibility(View.VISIBLE);
                btnAlight.setEnabled(true);
                return;
            }

            performCheckOut(location.getLatitude(), location.getLongitude());
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("Location error. Please try again.");
            tvStatus.setVisibility(View.VISIBLE);
            btnAlight.setEnabled(true);
        });
    }

    /**
     * Calls the controller to register the check-out, then loads the updated
     * trip to display the fare (req 4.1.1, 4.1.2, 4.1.4, 5.1.1, 5.1.3).
     */
    private void performCheckOut(double lat, double lng) {
        commuterTripController.checkOut(commuterTripId, lat, lng);

        btnAlight.setVisibility(View.GONE);
        tvStatus.setText("Checked out. Calculating fare…");
        tvStatus.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        // Load the updated trip to get distance and present fare
        loadTripAndPresentFare();
    }

    /**
     * Loads the commuter trip from Firebase after check-out to display the fare.
     */
    private void loadTripAndPresentFare() {
        FirebaseDatabase.getInstance()
                .getReference("commuter_trips")
                .child(commuterTripId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        CommuterTrip trip = snapshot.getValue(CommuterTrip.class);
                        if (trip == null) {
                            showError("Trip not found.");
                            return;
                        }
                        currentTrip = trip;
                        presentFare(trip);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        showError("Failed to load trip: " + error.getMessage());
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Fare presentation
    // -------------------------------------------------------------------------

    /**
     * Displays the calculated fare and shows the "Pay" button (req 5.1.1, 5.1.3).
     */
    private void presentFare(CommuterTrip trip) {
        String currency = trip.getCurrency() != null ? trip.getCurrency() : "ZAR";
        Fare fare = commuterTripController.calculateCommuterFare(trip.getDistanceKm(), currency);
        currentFare = fare;

        tvFareLabel.setText(String.format("Your fare (%s)", currency));
        tvFareAmount.setText(String.format(Locale.getDefault(), "%s %.2f",
                currency, fare.getAmountRand()));

        // Show the fare card (parent of tv_fare_label and tv_fare_amount)
        fareCard.setVisibility(View.VISIBLE);

        btnPay.setVisibility(View.VISIBLE);
    }

    // -------------------------------------------------------------------------
    // Payment flow (same pattern as PaymentActivity)
    // -------------------------------------------------------------------------

    /**
     * Called when the commuter taps "Pay". Initiates payment via Peach Payments
     * (req 5.2.1, 5.2.2).
     */
    private void onPayTapped() {
        if (currentTrip == null || currentFare == null) {
            return;
        }
        initiatePayment();
    }

    /**
     * Launches the Peach Payments checkout sheet.
     * On success calls {@link CommuterTripController#processCommuterPayment}.
     * On failure increments attempt counter; after 3 failures flags the payment.
     */
    private void initiatePayment() {
        paymentAttempts++;
        btnPay.setEnabled(false);
        tvStatus.setText("Processing payment…");
        tvStatus.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        launchPeachCheckout(currentFare, new PaymentResultCallback() {
            @Override
            public void onSuccess(String transactionId) {
                handlePaymentSuccess(transactionId);
            }

            @Override
            public void onFailure(String reason) {
                handlePaymentFailure(reason);
            }
        });
    }

    /**
     * Launches the Peach Payments UI sheet.
     *
     * <p>In production this calls {@code CheckoutActivity} from the Peach SDK after
     * obtaining a checkout ID from the {@code peachPaymentsProxy} Cloud Function.
     * The stub below simulates the SDK callback contract so the activity compiles
     * and the retry/flag logic is fully exercisable in unit tests.</p>
     */
    void launchPeachCheckout(Fare fare, PaymentResultCallback callback) {
        // TODO: replace stub with real Peach Payments SDK call once SDK is integrated.
        // Stub: simulate a failure so retry UI is exercisable during development.
        callback.onFailure("PEACH_SDK_NOT_INTEGRATED");
    }

    private void handlePaymentSuccess(String transactionId) {
        progressBar.setVisibility(View.GONE);
        tvStatus.setText("Payment successful.");
        tvStatus.setVisibility(View.VISIBLE);
        btnPay.setVisibility(View.GONE);

        commuterTripController.processCommuterPayment(currentTrip, true);

        Toast.makeText(this, "Payment confirmed. Trip complete.", Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Handles payment failure with retry UI following the same pattern as
     * {@link PaymentActivity} (req 5.2.2).
     */
    private void handlePaymentFailure(String reason) {
        progressBar.setVisibility(View.GONE);

        if (paymentAttempts >= MAX_PAYMENT_ATTEMPTS) {
            // All attempts exhausted — flag the payment (req 5.2.3, 5.2.4)
            commuterTripController.processCommuterPayment(currentTrip, false);
            tvStatus.setText("Payment could not be processed. Your trip has been flagged for review.");
            tvStatus.setVisibility(View.VISIBLE);
            btnPay.setVisibility(View.GONE);
        } else {
            // Show retry UI (same pattern as PaymentActivity)
            int remaining = MAX_PAYMENT_ATTEMPTS - paymentAttempts;
            tvStatus.setText(String.format(
                    Locale.getDefault(),
                    "Payment failed (%s). %d attempt%s remaining.",
                    reason, remaining, remaining == 1 ? "" : "s"));
            tvStatus.setVisibility(View.VISIBLE);
            btnPay.setEnabled(true);
            btnPay.setText("Retry Payment");
        }
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private void bindViews() {
        btnAlight = findViewById(R.id.btn_alight);
        tvFareAmount = findViewById(R.id.tv_fare_amount);
        tvFareLabel = findViewById(R.id.tv_fare_label);
        tvStatus = findViewById(R.id.tv_status);
        btnPay = findViewById(R.id.btn_pay);
        progressBar = findViewById(R.id.progress_bar);

        // The fare card is the parent MaterialCardView of tv_fare_label and tv_fare_amount
        fareCard = (MaterialCardView) tvFareLabel.getParent().getParent();
    }

    private void showError(String message) {
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
        btnPay.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onAlightTapped();
        } else {
            tvStatus.setText("Location permission is required to check out.");
            tvStatus.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (occupancyRef != null && occupancyListener != null) {
            occupancyRef.removeEventListener(occupancyListener);
        }
        if (tripRef != null && tripListener != null) {
            tripRef.removeEventListener(tripListener);
        }
    }

    // -------------------------------------------------------------------------
    // Inner callback interface
    // -------------------------------------------------------------------------

    /** Callback contract for Peach Payments checkout result. */
    interface PaymentResultCallback {
        void onSuccess(String transactionId);
        void onFailure(String reason);
    }
}
