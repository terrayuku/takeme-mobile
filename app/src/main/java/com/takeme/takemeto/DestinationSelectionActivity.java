package com.takeme.takemeto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.takeme.takemeto.impl.Location;
import com.takeme.takemeto.impl.TripControllerImpl;
import com.takeme.takemeto.model.Trip;

import androidx.annotation.NonNull;

/**
 * Destination selection screen shown after boarding confirmation (req 5.1.1, 5.1.2, 5.3.1, 5.3.2).
 *
 * <p>Receives {@code tripId} via Intent extra, loads the Trip from Firebase RTDB,
 * presents an autocomplete fragment for destination input, validates the destination
 * is within the operating zone, and on confirmation starts {@link PaymentActivity}.</p>
 */
public class DestinationSelectionActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_TRIP_ID = "tripId";

    // Operating zone bounding box — same as MainActivity / Location.java
    private static final double ZONE_LAT_MIN = -34.277857;
    private static final double ZONE_LAT_MAX = -23.9116035;
    private static final double ZONE_LNG_MIN = 18.2359139;
    private static final double ZONE_LNG_MAX = 29.380895;

    private static final float DEFAULT_ZOOM = 14f;

    private String tripId;
    private Trip trip;

    private TripControllerImpl tripController;
    private Location location;

    private GoogleMap googleMap;
    private Button btnConfirmDestination;
    private View rootLayout;

    private LatLng selectedDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destination_selection);

        tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        rootLayout = findViewById(R.id.destination_selection_root);
        btnConfirmDestination = findViewById(R.id.btn_confirm_destination);

        tripController = new TripControllerImpl(
                FirebaseDatabase.getInstance(),
                FirebaseRemoteConfig.getInstance());

        location = new Location();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.maps_key));
        }

        loadTripFromFirebase();
        setupAutocomplete();
        setupMap();

        btnConfirmDestination.setOnClickListener(v -> onConfirmDestination());
    }

    /** Loads the Trip record from /trips/{tripId} so we have the full object for TripController. */
    private void loadTripFromFirebase() {
        if (tripId == null) return;

        FirebaseDatabase.getInstance()
                .getReference("trips")
                .child(tripId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        trip = snapshot.getValue(Trip.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Trip load failed — proceed without it; TripController will handle null gracefully
                    }
                });
    }

    /** Sets up the AutocompleteSupportFragment using Location.setPlace() (same pattern as MainActivity). */
    private void setupAutocomplete() {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.destination_autocomplete);

        if (autocompleteFragment == null) return;

        location.setPlace(autocompleteFragment, "Enter destination...")
                .setOnPlaceSelectedListener(new PlaceSelectionListener() {
                    @Override
                    public void onPlaceSelected(@NonNull Place place) {
                        handlePlaceSelected(place);
                    }

                    @Override
                    public void onError(@NonNull Status status) {
                        // No-op — user can retry
                    }
                });
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
    }

    /**
     * Called when the user selects a place from autocomplete.
     * Validates the destination is within the operating zone (req 5.3.1, 5.3.2).
     */
    private void handlePlaceSelected(Place place) {
        LatLng latLng = place.getLatLng();
        if (latLng == null) return;

        if (!isWithinOperatingZone(latLng)) {
            // Destination outside service area — show Snackbar with nearest boundary suggestion (req 5.3.1, 5.3.2)
            LatLng nearest = nearestZoneBoundaryPoint(latLng);
            String suggestion = String.format("%.4f, %.4f", nearest.latitude, nearest.longitude);
            Snackbar.make(rootLayout,
                    "Destination is outside the service area. Nearest drop-off: " + suggestion,
                    Snackbar.LENGTH_LONG).show();

            // Hide confirm button and do NOT proceed to payment
            btnConfirmDestination.setVisibility(View.GONE);
            selectedDestination = null;
            return;
        }

        // Valid destination — update map and show confirm button
        selectedDestination = latLng;
        updateMap(latLng, place.getName());
        btnConfirmDestination.setVisibility(View.VISIBLE);
    }

    /** Updates the map camera and marker to show the selected destination. */
    private void updateMap(LatLng latLng, String name) {
        if (googleMap == null) return;
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(latLng).title(name));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
    }

    /**
     * Called when the user taps "Confirm Destination".
     * Calls TripController.onDestinationSelected() and starts PaymentActivity (req 5.1.2).
     */
    private void onConfirmDestination() {
        if (selectedDestination == null) return;

        if (trip != null) {
            tripController.onDestinationSelected(trip, selectedDestination.latitude, selectedDestination.longitude);
        }

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(EXTRA_TRIP_ID, tripId);
        startActivity(intent);
        finish();
    }

    /** Returns true if the given LatLng is within the operating zone bounding box. */
    private boolean isWithinOperatingZone(LatLng latLng) {
        return latLng.latitude >= ZONE_LAT_MIN
                && latLng.latitude <= ZONE_LAT_MAX
                && latLng.longitude >= ZONE_LNG_MIN
                && latLng.longitude <= ZONE_LNG_MAX;
    }

    /**
     * Returns the nearest point on the operating zone boundary to the given LatLng.
     * Used to suggest the nearest valid drop-off when destination is out of zone (req 5.3.2).
     */
    private LatLng nearestZoneBoundaryPoint(LatLng latLng) {
        double clampedLat = Math.max(ZONE_LAT_MIN, Math.min(ZONE_LAT_MAX, latLng.latitude));
        double clampedLng = Math.max(ZONE_LNG_MIN, Math.min(ZONE_LNG_MAX, latLng.longitude));
        return new LatLng(clampedLat, clampedLng);
    }
}
