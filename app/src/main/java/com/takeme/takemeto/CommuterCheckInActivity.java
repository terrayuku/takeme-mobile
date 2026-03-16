package com.takeme.takemeto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.takeme.takemeto.impl.CommuterTripController;
import com.takeme.takemeto.impl.CommuterTripControllerImpl;
import com.takeme.takemeto.model.VehicleOccupancyState;

/**
 * Commuter check-in screen shown when a boarding window is open on a vehicle.
 * Listens on /vehicle_occupancy/{vehicleId} for boardingWindowOpen state and
 * presents a countdown timer. Commuters can tap "Board" to check in via GPS
 * proximity validation, or "Scan QR" as a fallback (req 1.1.1, 1.1.4, 1.3.2, 8.1.2).
 */
public class CommuterCheckInActivity extends AppCompatActivity {

    public static final String EXTRA_VEHICLE_ID = "vehicleId";

    private static final long BOARDING_WINDOW_MILLIS = 90_000L;
    private static final long TICK_MILLIS = 1_000L;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private String vehicleId;

    private DatabaseReference occupancyRef;
    private ValueEventListener occupancyListener;
    private CountDownTimer countDownTimer;
    private FusedLocationProviderClient fusedLocationClient;
    private CommuterTripController commuterTripController;

    private TextView tvStatus;
    private TextView tvBoardingCountdown;
    private ProgressBar progressBar;
    private Button btnBoard;
    private Button btnScanQr;

    /** Latest occupancy snapshot from Firebase. */
    private VehicleOccupancyState latestOccupancy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commuter_check_in);

        vehicleId = getIntent().getStringExtra(EXTRA_VEHICLE_ID);

        tvStatus = findViewById(R.id.tv_status);
        tvBoardingCountdown = findViewById(R.id.tv_boarding_countdown);
        progressBar = findViewById(R.id.progress_bar);
        btnBoard = findViewById(R.id.btn_board);
        btnScanQr = findViewById(R.id.btn_scan_qr);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        commuterTripController = new CommuterTripControllerImpl(
                FirebaseDatabase.getInstance(),
                FirebaseRemoteConfig.getInstance());

        // Listen on /vehicle_occupancy/{vehicleId} for boarding window state (req 1.3.2)
        occupancyRef = FirebaseDatabase.getInstance()
                .getReference("vehicle_occupancy")
                .child(vehicleId);

        occupancyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                latestOccupancy = snapshot.getValue(VehicleOccupancyState.class);
                updateUiFromOccupancy();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silently handle — UI remains in last known state
            }
        };
        occupancyRef.addValueEventListener(occupancyListener);

        // Start boarding window countdown (req 1.3.2)
        startBoardingCountdown();

        // "Board" button — GPS proximity check-in (req 1.1.1)
        btnBoard.setOnClickListener(v -> onBoardTapped());

        // "Scan QR" button — launch QR fallback activity (req 1.2.1)
        btnScanQr.setOnClickListener(v -> onScanQrTapped());
    }

    /** Updates UI elements based on the latest occupancy state from Firebase. */
    private void updateUiFromOccupancy() {
        if (latestOccupancy == null) {
            return;
        }

        if (!latestOccupancy.isBoardingWindowOpen()) {
            tvStatus.setText("Boarding window is closed.");
            btnBoard.setEnabled(false);
            cancelTimer();
        }

        // Capacity full check (req 8.1.2)
        if (latestOccupancy.getCheckedInCount() >= latestOccupancy.getMaxCapacity()) {
            tvStatus.setText("Vehicle is at full capacity.");
            btnBoard.setEnabled(false);
        }
    }

    /** Starts the 90-second boarding window countdown timer. */
    private void startBoardingCountdown() {
        countDownTimer = new CountDownTimer(BOARDING_WINDOW_MILLIS, TICK_MILLIS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                tvBoardingCountdown.setText(String.valueOf(secondsLeft));
            }

            @Override
            public void onFinish() {
                tvBoardingCountdown.setText("0");
                tvStatus.setText("Boarding window has closed.");
                btnBoard.setEnabled(false);
            }
        };
        countDownTimer.start();
    }

    /**
     * Called when the commuter taps "Board". Gets current GPS location,
     * validates proximity ≤30m from vehicle, and calls checkIn (req 1.1.1).
     */
    private void onBoardTapped() {
        // Check capacity before proceeding (req 8.1.2)
        if (latestOccupancy != null
                && latestOccupancy.getCheckedInCount() >= latestOccupancy.getMaxCapacity()) {
            Toast.makeText(this, "Vehicle is full", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnBoard.setEnabled(false);

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            progressBar.setVisibility(View.GONE);

            if (location == null) {
                tvStatus.setText("Unable to get your location. Please try again.");
                btnBoard.setEnabled(true);
                return;
            }

            double commuterLat = location.getLatitude();
            double commuterLng = location.getLongitude();

            // Check for duplicate check-in before calling controller (req 1.1.4)
            checkDuplicateAndPerformCheckIn(commuterLat, commuterLng);
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("Location error. Please try again.");
            btnBoard.setEnabled(true);
        });
    }

    /**
     * Checks for an existing active trip on this vehicle before performing check-in.
     * Shows a duplicate rejection Toast if the commuter is already checked in (req 1.1.4).
     */
    private void checkDuplicateAndPerformCheckIn(double lat, double lng) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("commuter_trips")
                .orderByChild("commuterUid")
                .equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            com.takeme.takemeto.model.CommuterTrip existing =
                                    child.getValue(com.takeme.takemeto.model.CommuterTrip.class);
                            if (existing != null
                                    && vehicleId.equals(existing.getVehicleId())
                                    && isActiveTrip(existing)) {
                                // Duplicate — already checked in (req 1.1.4)
                                Toast.makeText(CommuterCheckInActivity.this,
                                        "Already checked in on this vehicle",
                                        Toast.LENGTH_SHORT).show();
                                btnBoard.setEnabled(true);
                                return;
                            }
                        }
                        // No duplicate — proceed with check-in
                        performCheckIn(lat, lng);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // On query failure, proceed with check-in; controller
                        // also validates duplicates server-side.
                        performCheckIn(lat, lng);
                    }
                });
    }

    /** Returns true if the trip is still active (BOARDING or IN_TRANSIT). */
    private boolean isActiveTrip(com.takeme.takemeto.model.CommuterTrip trip) {
        String status = trip.getStatus();
        return com.takeme.takemeto.model.CommuterTrip.CommuterTripStatus.BOARDING.name().equals(status)
                || com.takeme.takemeto.model.CommuterTrip.CommuterTripStatus.IN_TRANSIT.name().equals(status);
    }

    /** Launches QrCheckInActivity for QR code fallback check-in (req 1.2.1). */
    private void onScanQrTapped() {
        try {
            Class<?> qrClass = Class.forName("com.takeme.takemeto.QrCheckInActivity");
            Intent intent = new Intent(this, qrClass);
            intent.putExtra(EXTRA_VEHICLE_ID, vehicleId);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            Toast.makeText(this, "QR check-in is not available yet", Toast.LENGTH_SHORT).show();
        }
    }

    /** Calls the controller to register the check-in (req 1.1.1). */
    private void performCheckIn(double lat, double lng) {
        commuterTripController.checkIn(vehicleId, lat, lng);
        tvStatus.setText("Checked in! Have a safe trip.");
        btnBoard.setEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onBoardTapped();
        } else {
            tvStatus.setText("Location permission is required to board.");
        }
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
        if (occupancyRef != null && occupancyListener != null) {
            occupancyRef.removeEventListener(occupancyListener);
        }
    }
}
