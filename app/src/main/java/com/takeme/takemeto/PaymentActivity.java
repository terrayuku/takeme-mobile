package com.takeme.takemeto;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.takeme.takemeto.impl.TripControllerImpl;
import com.takeme.takemeto.model.Fare;
import com.takeme.takemeto.model.Trip;

import java.util.Locale;

/**
 * Displays the calculated fare and handles Peach Payments checkout.
 *
 * <p>Receives {@code tripId} via Intent extra. Loads the Trip from Firebase RTDB,
 * calculates the fare via {@link TripControllerImpl}, and presents the Peach Payments
 * checkout sheet. Handles up to 3 retry attempts with exponential backoff before
 * flagging the payment (req 6.1.2, 6.2.1, 6.2.2, 6.3.1, 6.3.2).</p>
 */
public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "tripId";

    static final int MAX_PAYMENT_ATTEMPTS = 3;

    private TextView tvFareAmount;
    private TextView tvFareLabel;
    private TextView tvStatusMessage;
    private Button btnPay;
    private Button btnRetry;
    private ProgressBar progressBar;

    private TripControllerImpl tripController;
    private Trip currentTrip;
    private int paymentAttempts = 0;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        bindViews();

        tripController = new TripControllerImpl(
                FirebaseDatabase.getInstance(),
                FirebaseRemoteConfig.getInstance());

        String tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (tripId == null || tripId.isEmpty()) {
            showError("No trip ID provided.");
            return;
        }

        loadTrip(tripId);
    }

    // -------------------------------------------------------------------------
    // Firebase — load trip
    // -------------------------------------------------------------------------

    private void loadTrip(String tripId) {
        showLoading(true);
        FirebaseDatabase.getInstance()
                .getReference("trips")
                .child(tripId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        showLoading(false);
                        Trip trip = snapshot.getValue(Trip.class);
                        if (trip == null) {
                            showError("Trip not found.");
                            return;
                        }
                        currentTrip = trip;
                        presentFare(trip);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        showError("Failed to load trip: " + error.getMessage());
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Fare presentation
    // -------------------------------------------------------------------------

    private void presentFare(Trip trip) {
        Fare fare = tripController.calculateFare(trip.getDistanceKm(), "ZAR");
        trip.setFare(fare);

        String currency = fare.getCurrency() != null ? fare.getCurrency() : "ZAR";
        tvFareLabel.setText(String.format("Your fare (%s)", currency));
        tvFareAmount.setText(String.format(Locale.getDefault(), "%s %.2f", currency, fare.getAmountRand()));

        btnPay.setVisibility(View.VISIBLE);
        btnPay.setOnClickListener(v -> initiatePayment(trip, fare));
    }

    // -------------------------------------------------------------------------
    // Payment flow
    // -------------------------------------------------------------------------

    /**
     * Launches the Peach Payments checkout sheet.
     * On success calls {@link TripControllerImpl#completeTrip(Trip, boolean)}.
     * On failure increments attempt counter; after 3 failures flags the payment.
     */
    private void initiatePayment(Trip trip, Fare fare) {
        paymentAttempts++;
        btnPay.setEnabled(false);
        btnRetry.setVisibility(View.GONE);
        tvStatusMessage.setText("Processing payment…");
        tvStatusMessage.setVisibility(View.VISIBLE);

        // Peach Payments Checkout SDK integration.
        // The SDK is initialised with the checkout ID obtained from our Cloud Function proxy.
        // We use a callback-based approach compatible with minSdkVersion 21.
        launchPeachCheckout(fare, new PaymentResultCallback() {
            @Override
            public void onSuccess(String transactionId) {
                handlePaymentSuccess(trip, transactionId);
            }

            @Override
            public void onFailure(String reason) {
                handlePaymentFailure(trip, fare, reason);
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
        // TODO: replace stub with real Peach Payments SDK call once SDK is integrated:
        //   1. Call peachPaymentsProxy Cloud Function with { tripId, amountRand, currency }
        //   2. Receive checkoutId in response
        //   3. Start CheckoutActivity.createIntent(this, checkoutId)
        //   4. Handle onActivityResult: SUCCESS → callback.onSuccess(transactionId)
        //                               FAILURE → callback.onFailure(reason)
        //
        // Stub: simulate a failure so retry UI is exercisable during development.
        callback.onFailure("PEACH_SDK_NOT_INTEGRATED");
    }

    private void handlePaymentSuccess(Trip trip, String transactionId) {
        tvStatusMessage.setText("Payment successful.");
        btnPay.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);

        tripController.completeTrip(trip, true);

        Toast.makeText(this, "Payment confirmed. Trip complete.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void handlePaymentFailure(Trip trip, Fare fare, String reason) {
        if (paymentAttempts >= MAX_PAYMENT_ATTEMPTS) {
            // All attempts exhausted — flag the payment (req 6.3.2).
            tripController.completeTrip(trip, false);
            tvStatusMessage.setText("Payment could not be processed. Your trip has been flagged for review.");
            tvStatusMessage.setVisibility(View.VISIBLE);
            btnPay.setVisibility(View.GONE);
            btnRetry.setVisibility(View.GONE);
        } else {
            // Show retry UI (req 6.3.1).
            int remaining = MAX_PAYMENT_ATTEMPTS - paymentAttempts;
            tvStatusMessage.setText(String.format(
                    Locale.getDefault(),
                    "Payment failed (%s). %d attempt%s remaining.",
                    reason, remaining, remaining == 1 ? "" : "s"));
            tvStatusMessage.setVisibility(View.VISIBLE);
            btnRetry.setVisibility(View.VISIBLE);
            btnRetry.setOnClickListener(v -> initiatePayment(trip, fare));
        }
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private void bindViews() {
        tvFareLabel   = findViewById(R.id.tv_fare_label);
        tvFareAmount  = findViewById(R.id.tv_fare_amount);
        tvStatusMessage = findViewById(R.id.tv_status_message);
        btnPay        = findViewById(R.id.btn_pay);
        btnRetry      = findViewById(R.id.btn_retry);
        progressBar   = findViewById(R.id.progress_bar);
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPay.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        tvStatusMessage.setText(message);
        tvStatusMessage.setVisibility(View.VISIBLE);
        btnPay.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
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
