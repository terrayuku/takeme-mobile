package com.takeme.takemeto;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Boarding confirmation screen shown to the passenger when the taxi arrives (req 4.1.1).
 * Listens on /pickup_requests/{requestId} and presents a 60-second countdown.
 * On confirm: writes status=CONFIRMED and starts DestinationSelectionActivity (req 4.2.1).
 * On timeout: writes status=UNCONFIRMED and finishes (req 4.3.1, 4.3.2).
 */
public class BoardingConfirmationActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "requestId";
    public static final String EXTRA_VEHICLE_ID = "vehicleId";
    public static final String EXTRA_TRIP_ID    = "tripId";

    private static final long COUNTDOWN_MILLIS = 60_000L;
    private static final long TICK_MILLIS       = 1_000L;

    private String requestId;
    private String vehicleId;

    private DatabaseReference pickupRequestRef;
    private ValueEventListener pickupRequestListener;
    private CountDownTimer countDownTimer;

    private TextView tvCountdown;
    private Button btnConfirmBoarding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding_confirmation);

        requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        vehicleId = getIntent().getStringExtra(EXTRA_VEHICLE_ID);

        tvCountdown        = findViewById(R.id.tv_countdown);
        btnConfirmBoarding = findViewById(R.id.btn_confirm_boarding);

        // Listen on /pickup_requests/{requestId} (req 4.1.1)
        pickupRequestRef = FirebaseDatabase.getInstance()
                .getReference("pickup_requests")
                .child(requestId);

        pickupRequestListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // React to server-side status changes if needed in future iterations
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // No-op for now; activity will still time out normally
            }
        };
        pickupRequestRef.addValueEventListener(pickupRequestListener);

        // 60-second countdown (req 4.3.1)
        countDownTimer = new CountDownTimer(COUNTDOWN_MILLIS, TICK_MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                tvCountdown.setText(String.valueOf(secondsLeft));
            }

            @Override
            public void onFinish() {
                tvCountdown.setText("0");
                onBoardingTimeout();
            }
        };
        countDownTimer.start();

        // Confirm boarding button (req 4.2.1)
        btnConfirmBoarding.setOnClickListener(v -> onBoardingConfirmed());
    }

    /** Called when the passenger taps "Confirm Boarding". */
    private void onBoardingConfirmed() {
        cancelTimer();

        // Write CONFIRMED status to RTDB
        pickupRequestRef.child("status").setValue("CONFIRMED");

        // Retrieve tripId from the snapshot to pass to DestinationSelectionActivity
        pickupRequestRef.child("tripId").get().addOnSuccessListener(snapshot -> {
            String tripId = snapshot.getValue(String.class);
            Intent intent = new Intent(this, DestinationSelectionActivity.class);
            if (tripId != null) {
                intent.putExtra(EXTRA_TRIP_ID, tripId);
            }
            startActivity(intent);

            // Launch multi-commuter check-in flow after boarding confirmation (req 1.1.1)
            launchCommuterCheckIn();

            finish();
        }).addOnFailureListener(e -> {
            // Even if tripId fetch fails, navigate forward without it
            Intent intent = new Intent(this, DestinationSelectionActivity.class);
            startActivity(intent);

            // Launch multi-commuter check-in flow after boarding confirmation (req 1.1.1)
            launchCommuterCheckIn();

            finish();
        });
    }

    /**
     * Launches CommuterCheckInActivity to begin the multi-commuter boarding flow
     * after the initial single-passenger boarding confirmation completes (req 1.1.1).
     */
    private void launchCommuterCheckIn() {
        if (vehicleId != null) {
            Intent checkInIntent = new Intent(this, CommuterCheckInActivity.class);
            checkInIntent.putExtra(CommuterCheckInActivity.EXTRA_VEHICLE_ID, vehicleId);
            startActivity(checkInIntent);
        }
    }

    /** Called when the 60-second countdown expires without confirmation. */
    private void onBoardingTimeout() {
        // Write UNCONFIRMED status to RTDB (req 4.3.1)
        pickupRequestRef.child("status").setValue("UNCONFIRMED");
        finish();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel timer and remove Firebase listener to avoid leaks (req 4.3.2)
        cancelTimer();
        if (pickupRequestRef != null && pickupRequestListener != null) {
            pickupRequestRef.removeEventListener(pickupRequestListener);
        }
    }
}
