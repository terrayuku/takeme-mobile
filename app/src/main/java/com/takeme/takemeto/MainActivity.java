package com.takeme.takemeto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.takeme.takemeto.impl.Analytics;
import com.takeme.takemeto.impl.Location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import io.fabric.sdk.android.Fabric;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String EXTRA_MESSAGE = "com.takeme.takemeto.MESSAGE";
    public static final String FROM = "com.takeme.takemeto.FROM";
    public static final String DESTINATION = "com.takeme.takemeto.DESTINATION";
    TextView thankyou;
    FloatingActionButton findDirections;
    private AdView mAdView;

    Place from;
    Place destination;
    private FirebaseAnalytics firebaseAnalytics;
    private Analytics analytics;
    private FirebaseAuth auth;
    Location location;

    public static final String TAG = "MainActivity";
    private View mLayout;


    private static final int REQUEST_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        auth = FirebaseAuth.getInstance();

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        analytics = new Analytics();
        analytics.setAnalytics(firebaseAnalytics, "App Open", "App Open", "App Open");

        initializePlaces();

        if(auth.getCurrentUser() != null) {
            setContentView(R.layout.activity_main);
        } else {
            finish();
        }

        mAdView = findViewById(R.id.adMain);
        findDirections = (FloatingActionButton) findViewById(R.id.findDirections);
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

        final Intent addSingIntent = new Intent(this, AddSignForDirections.class);

        FloatingActionButton fab = findViewById(R.id.addDirections);
        if(fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(auth.getCurrentUser().isEmailVerified()) {
                        analytics.setAnalytics(firebaseAnalytics, "Add Sign", "Add", "Add Sign");
                        startActivity(addSingIntent);
                    } else {
                        Snackbar.make(mLayout, R.string.emailVerification,
                                Snackbar.LENGTH_SHORT).show();
                    }

                }
            });
        }

        AutocompleteSupportFragment toFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.destination);

        AutocompleteSupportFragment fromFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.from);

        if(fromFragment != null && toFragment != null) {
            // E/Places: Error while autocompleting: TIMEOU
            location.setPlace(fromFragment, "From...").setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    analytics.setAnalytics(firebaseAnalytics, "From", fromFragment.getTag(), "Place Found");
                    from = place;
                }

                @Override
                public void onError(@NonNull Status status) {
                    analytics.setAnalytics(firebaseAnalytics, "From", fromFragment.getTag(), "Place Not Found");
                }
            });

            location.setPlace(toFragment, "To...").setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    analytics.setAnalytics(firebaseAnalytics, "To", toFragment.getTag(), "Place Found");
                    destination = place;
                }

                @Override
                public void onError(@NonNull Status status) {
                    analytics.setAnalytics(firebaseAnalytics, "To", toFragment.getTag(), "Place Not Found");
                }
            });
        }

        loadAdView();

    }

    private void initializePlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.maps_key));
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
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser == null) {
            // Already signed in
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

        if (from == null || destination == null) {
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
            String message = "From " + from.getName() + " To " + destination.getName();
            analytics.setAnalytics(firebaseAnalytics, "Directions", "Directions", "Search Directions Entered");
            findSignIntent.putExtra(EXTRA_MESSAGE, message);
            findSignIntent.putExtra(DESTINATION, destination.getName());
            findSignIntent.putExtra(FROM, from.getName());
            startActivity(findSignIntent);
        }

    }

    private void error() {
        thankyou.setText(getResources().getString(R.string.genericFailure));
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
}
