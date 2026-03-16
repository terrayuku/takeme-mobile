package com.takeme.takemeto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.takeme.takemeto.impl.CommuterTripController;
import com.takeme.takemeto.impl.CommuterTripControllerImpl;
import com.takeme.takemeto.model.CommuterTrip;

/**
 * QR code fallback check-in activity. Scans a vehicle-specific QR code
 * containing the vehicleId, validates it matches the expected vehicle from
 * the Intent extra, and registers a check-in with checkinMethod = "QR_CODE".
 *
 * Requirements: 1.2.1, 1.2.2, 1.2.3
 */
public class QrCheckInActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 2001;

    private String expectedVehicleId;
    private boolean scanProcessed;

    private DecoratedBarcodeView barcodeScanner;
    private TextView tvStatus;
    private Button btnCancel;

    private FusedLocationProviderClient fusedLocationClient;
    private CommuterTripController commuterTripController;

    private final BarcodeCallback barcodeCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (scanProcessed) {
                return;
            }

            String payload = result.getText();
            if (payload == null || payload.trim().isEmpty()) {
                tvStatus.setText("Invalid QR code");
                return;
            }

            String scannedVehicleId = payload.trim();

            // Validate scanned vehicleId matches the expected vehicle (req 1.2.2)
            if (!scannedVehicleId.equals(expectedVehicleId)) {
                tvStatus.setText("QR code does not match nearby vehicle");
                // Pause briefly then resume scanning so the user can try again
                barcodeScanner.setStatusText("Try scanning again");
                return;
            }

            // Valid QR — stop scanning and perform check-in
            scanProcessed = true;
            barcodeScanner.pause();
            tvStatus.setText("QR code recognised. Checking in…");
            performQrCheckIn(scannedVehicleId);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_check_in);

        expectedVehicleId = getIntent().getStringExtra(
                CommuterCheckInActivity.EXTRA_VEHICLE_ID);

        barcodeScanner = findViewById(R.id.barcode_scanner);
        tvStatus = findViewById(R.id.tv_status);
        btnCancel = findViewById(R.id.btn_cancel);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        commuterTripController = new CommuterTripControllerImpl(
                FirebaseDatabase.getInstance(),
                FirebaseRemoteConfig.getInstance());

        barcodeScanner.decodeContinuous(barcodeCallback);

        btnCancel.setOnClickListener(v -> finish());
    }

    /**
     * Gets the commuter's current GPS location and calls checkIn with
     * checkinMethod = "QR_CODE" (req 1.2.2, 1.2.3).
     */
    private void performQrCheckIn(String vehicleId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location == null) {
                tvStatus.setText("Unable to get your location. Please try again.");
                resetScanner();
                return;
            }

            commuterTripController.checkIn(
                    vehicleId,
                    location.getLatitude(),
                    location.getLongitude(),
                    CommuterTrip.CheckinMethod.QR_CODE.name());

            tvStatus.setText("Checked in via QR! Have a safe trip.");
            Toast.makeText(this, "Checked in via QR code", Toast.LENGTH_SHORT).show();

            // Close after a short delay so the user sees the confirmation
            tvStatus.postDelayed(this::finish, 1500);
        }).addOnFailureListener(e -> {
            tvStatus.setText("Location error. Please try again.");
            resetScanner();
        });
    }

    /** Re-enables scanning after a failed attempt. */
    private void resetScanner() {
        scanProcessed = false;
        barcodeScanner.resume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            performQrCheckIn(expectedVehicleId);
        } else {
            tvStatus.setText("Location permission is required to check in.");
            resetScanner();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeScanner.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScanner.pause();
    }
}
