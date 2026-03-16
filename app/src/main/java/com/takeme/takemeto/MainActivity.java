package com.takeme.takemeto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.takeme.takemeto.impl.Analytics;
import com.takeme.takemeto.impl.Location;
import com.takeme.takemeto.impl.PlacesAutoCompleteAdapter;
import com.takeme.takemeto.model.CommuterTrip;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;


import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String EXTRA_MESSAGE = "com.takeme.takemeto.MESSAGE";
    public static final String FROM = "com.takeme.takemeto.FROM";
    public static final String DESTINATION = "com.takeme.takemeto.DESTINATION";
    TextView thankyou;
    ExtendedFloatingActionButton findDirections;
    private AdView mAdView;

    private PlacesClient placesClient;
    private String fromName;
    private String destinationName;
    private FirebaseAnalytics firebaseAnalytics;
    private Analytics analytics;
    private FirebaseAuth auth;
    Location location;

    // Commuter trip history (req 6.3.2)
    private LinearLayout tripHistoryContainer;
    private TextView tripHistoryLabel;
    private TextView noTripsText;
    private Query commuterTripsQuery;
    private ValueEventListener commuterTripsListener;

    public static final String TAG = "MainActivity";
    private View mLayout;


    private static final int REQUEST_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (!BuildConfig.DEBUG && auth.getCurrentUser() == null) {
            loadLoginActivity();
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(this);
            analytics = new Analytics();
            analytics.setAnalytics(firebaseAnalytics, "App Open", "App Open", "App Open");
        } catch (Exception e) {
            // Analytics may fail in debug without full Firebase setup
            analytics = new Analytics();
        }

        initializePlaces();
        placesClient = Places.createClient(this);

        mAdView = findViewById(R.id.adMain);
        findDirections = (ExtendedFloatingActionButton) findViewById(R.id.findDirections);
        thankyou = (TextView) findViewById(R.id.thankyou);
        mLayout = thankyou;

        location = new Location();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        displayMessage(intent);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
        }

        // Commuter trip history (req 6.3.2)
        tripHistoryLabel = findViewById(R.id.tv_trip_history_label);
        tripHistoryContainer = findViewById(R.id.trip_history_container);
        noTripsText = findViewById(R.id.tv_no_trips);
        loadCommuterTripHistory();

        final Intent addSingIntent = new Intent(this, AddSignForDirections.class);

        FloatingActionButton fab = findViewById(R.id.addDirections);
        if(fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseUser user = auth.getCurrentUser();
                    if(user != null && user.isEmailVerified()) {
                        analytics.setAnalytics(firebaseAnalytics, "Add Sign", "Add", "Add Sign");
                        startActivity(addSingIntent);
                    } else if (BuildConfig.DEBUG) {
                        // Allow in debug without email verification
                        analytics.setAnalytics(firebaseAnalytics, "Add Sign", "Add", "Add Sign");
                        startActivity(addSingIntent);
                    } else {
                        Snackbar.make(mLayout, R.string.emailVerification,
                                Snackbar.LENGTH_SHORT).show();
                    }

                }
            });
        }

        // Inline autocomplete setup
        PlacesAutoCompleteAdapter fromAdapter = new PlacesAutoCompleteAdapter(this, placesClient);
        PlacesAutoCompleteAdapter toAdapter = new PlacesAutoCompleteAdapter(this, placesClient);

        AutoCompleteTextView fromInput = findViewById(R.id.from);
        AutoCompleteTextView toInput = findViewById(R.id.destination);

        fromInput.setAdapter(fromAdapter);
        fromInput.setOnItemClickListener((parent, view, position, id) -> {
            fromName = fromAdapter.getItem(position).getPrimaryText(null).toString();
            analytics.setAnalytics(firebaseAnalytics, "From", fromName, "Place Found");
        });

        toInput.setAdapter(toAdapter);
        toInput.setOnItemClickListener((parent, view, position, id) -> {
            destinationName = toAdapter.getItem(position).getPrimaryText(null).toString();
            analytics.setAnalytics(firebaseAnalytics, "To", destinationName, "Place Found");
        });

        loadAdView();

    }

    private void initializePlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.maps_key).trim());
        }
    }

    private AutocompleteSupportFragment setPlace(AutocompleteSupportFragment fragment, String hint) {
        fragment.setHint(hint);
        fragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));

        fragment.setLocationBias(RectangularBounds.newInstance(
                new LatLng(-34.277857, 18.2359139),
                new LatLng(-23.9116035, 29.380895)));

        return fragment;
    }

    private void requestLocationPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(mLayout, R.string.permission_location_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_LOCATION);
                        }
                    })
                    .show();
        } else {

            // Camera permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_LOCATION) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for location permission.

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                Snackbar.make(mLayout, R.string.permision_available_location,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        }  else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!BuildConfig.DEBUG && auth.getCurrentUser() == null) {
            loadLoginActivity();
        }
    }
    private void loadLoginActivity() {
        Intent findSignIntent = new Intent(this, LoginActivity.class);
        startActivity(findSignIntent);
    }

    private void loadAdView() {

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        AdRequest adRequest = new AdRequest.Builder()
                .addKeyword(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        try {
            mAdView.loadAd(adRequest);
        } catch (NullPointerException npe) {
            return;
        }
    }

    private void displayMessage(Intent intent) {
        if (intent != null) {

            SpannableStringBuilder spannableStringBuilder = null;
            if (intent.getStringExtra(DisplaySignActivity.THANKYOU) != null) {

                spannableStringBuilder = location.message(DisplaySignActivity.THANKYOU, intent);
                thankyou.setText(spannableStringBuilder);
                analytics.setAnalytics(firebaseAnalytics, "Thank You Message", "Thank you", "Display Sign Activity Thank You");

            } else if (intent.getStringExtra(DisplaySignActivity.SIGN_NOT_FOUND) != null) {

                spannableStringBuilder = location.message(DisplaySignActivity.SIGN_NOT_FOUND, intent);
                thankyou.setText(spannableStringBuilder);
                analytics.setAnalytics(firebaseAnalytics, "Sign Not Found", "Sign Not Found", "Sign Not Found");

            } else {
                analytics.setAnalytics(firebaseAnalytics, "No Action", "No Action", "No Action");
            }
        } else {
            error();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items destination the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.about) {
            Intent findSignIntent = new Intent(this, AboutActivity.class);
            startActivity(findSignIntent);
            return true;
        } else if (id == R.id.logout) {
            logout();
        } else if(id == R.id.settings) {
            Intent findSignIntent = new Intent(this, SettingsActivity.class);
            startActivity(findSignIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void findSingButton(View view) {
        Intent findSignIntent = new Intent(this, DisplaySignActivity.class);

        if (fromName == null || fromName.isEmpty() || destinationName == null || destinationName.isEmpty()) {
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(getResources().getString(R.string.noValidDirections));
            spannableStringBuilder.setSpan(
                    new ForegroundColorSpan(Color.RED),
                    0,
                    getResources().getString(R.string.noValidDirections).length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
            );
            thankyou.setText(spannableStringBuilder);
            analytics.setAnalytics(firebaseAnalytics, "Directions", "Directions", "No Valid Directions Entered");
        } else {
            String message = "From " + fromName + " To " + destinationName;
            analytics.setAnalytics(firebaseAnalytics, "Directions", "Directions", "Search Directions Entered");
            findSignIntent.putExtra(EXTRA_MESSAGE, message);
            findSignIntent.putExtra(DESTINATION, destinationName);
            findSignIntent.putExtra(FROM, fromName);
            startActivity(findSignIntent);
        }

    }

    private void error() {
        thankyou.setText(getResources().getString(R.string.genericFailure));
    }

    /**
     * Loads commuter trip history from Firebase RTDB filtered by the current
     * user's UID. Displays trips in the trip history container (req 6.3.2).
     */
    private void loadCommuterTripHistory() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String currentUserUid = user.getUid();
        commuterTripsQuery = FirebaseDatabase.getInstance()
                .getReference("commuter_trips")
                .orderByChild("commuterUid")
                .equalTo(currentUserUid);

        commuterTripsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tripHistoryLabel.setVisibility(View.VISIBLE);
                tripHistoryContainer.removeAllViews();

                if (!snapshot.exists() || !snapshot.hasChildren()) {
                    tripHistoryContainer.setVisibility(View.GONE);
                    noTripsText.setVisibility(View.VISIBLE);
                    return;
                }

                noTripsText.setVisibility(View.GONE);
                tripHistoryContainer.setVisibility(View.VISIBLE);

                for (DataSnapshot tripSnapshot : snapshot.getChildren()) {
                    CommuterTrip trip = tripSnapshot.getValue(CommuterTrip.class);
                    if (trip != null) {
                        addTripItemView(trip);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silently handle — trip history is non-critical
            }
        };

        commuterTripsQuery.addValueEventListener(commuterTripsListener);
    }

    private void addTripItemView(CommuterTrip trip) {
        View itemView = LayoutInflater.from(this)
                .inflate(R.layout.item_commuter_trip, tripHistoryContainer, false);

        TextView statusText = itemView.findViewById(R.id.tv_trip_status);
        TextView fareText = itemView.findViewById(R.id.tv_trip_fare);
        TextView distanceText = itemView.findViewById(R.id.tv_trip_distance);
        TextView dateText = itemView.findViewById(R.id.tv_trip_date);

        String status = trip.getStatus() != null ? trip.getStatus() : "UNKNOWN";
        statusText.setText(status);

        fareText.setText(String.format(Locale.getDefault(),
                getString(R.string.trip_fare_format), trip.getFareAmount()));

        distanceText.setText(String.format(Locale.getDefault(),
                getString(R.string.trip_distance_format), trip.getDistanceKm()));

        if (trip.getBoardingTimestampMs() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            dateText.setText(sdf.format(new Date(trip.getBoardingTimestampMs())));
        }

        tripHistoryContainer.addView(itemView);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
        finish();
    }

    private void logout() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        FirebaseAuth.getInstance().signOut();
        loadLoginActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commuterTripsQuery != null && commuterTripsListener != null) {
            commuterTripsQuery.removeEventListener(commuterTripsListener);
        }
    }
}
