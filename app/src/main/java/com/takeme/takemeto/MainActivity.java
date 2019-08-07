package com.takeme.takemeto;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.takeme.takemeto.impl.Analytics;
import com.takeme.takemeto.impl.Location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.takeme.takemeto.MESSAGE";
    public static final String FROM = "com.takeme.takemeto.FROM";
    public static final String DESTINATION = "com.takeme.takemeto.DESTINATION";
    TextView thankyou;
    FloatingActionButton findDirections;
    private AdView mAdView;
    Location location;

    Place from;
    Place destination;
    private FirebaseAnalytics firebaseAnalytics;
    private Analytics analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        location = new Location();

        loadAdView();
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        analytics = new Analytics();
        analytics.setAnalytics(firebaseAnalytics, "App Open", "App Open", "App Open");

        findDirections = (FloatingActionButton) findViewById(R.id.findDirections);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        thankyou = (TextView)findViewById(R.id.thankyou);
        Intent intent = getIntent();
        displayMessage(intent);

        final Intent addSingIntent = new Intent(this, AddSignForDirections.class);

        FloatingActionButton fab = findViewById(R.id.addDirections);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                analytics.setAnalytics(firebaseAnalytics, "Add Sign", "Add", "Add Sign");
                startActivity(addSingIntent);
            }
        });

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.maps_key));
        }
//        PlacesClient placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment fromFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.from);
        AutocompleteSupportFragment toFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.destination);

        if (fromFragment != null) {
            fromFragment.setHint("From...");
            fromFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));

            // Setting Bounds
            fromFragment.setLocationBias(RectangularBounds.newInstance(
                    new LatLng(-34.277857,18.2359139),
                    new LatLng(-23.9116035,29.380895)));

            // Set up a PlaceSelectionListener destination handle the response.
            fromFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    analytics.setAnalytics(firebaseAnalytics, "From Search", "From", "Place Found");
                    from = place;
                }

                @Override
                public void onError(@NonNull Status status) {
                    analytics.setAnalytics(firebaseAnalytics, "From Search", "From", "Place Not Found");
                }
            });
        }

        if (toFragment != null) {
            toFragment.setHint("To...");
            toFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));

            toFragment.setLocationBias(RectangularBounds.newInstance(
                    new LatLng(-34.277857,18.2359139),
                    new LatLng(-23.9116035,29.380895)));

            toFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    analytics.setAnalytics(firebaseAnalytics, "Destination Search", "Destination", "Place Found");
                    destination = place;
                }

                @Override
                public void onError(@NonNull Status status) {
                    analytics.setAnalytics(firebaseAnalytics, "Destination Search", "Destination", "Place Not Found");
                }
            });
        }
    }

    private void loadAdView() {

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adMain);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mAdView.loadAd(adRequest);
    }

    private void displayMessage(Intent intent) {
        if(intent != null) {
            String message = "";
            SpannableStringBuilder spannableStringBuilder = null; // = new SpannableStringBuilder(message);
            if(intent.getStringExtra(DisplaySignActivity.THANKYOU) != null) {

                message = intent.getStringExtra(DisplaySignActivity.THANKYOU);
                spannableStringBuilder = new SpannableStringBuilder(message);
                spannableStringBuilder.setSpan(
                        new ForegroundColorSpan(Color.BLACK),
                        0,
                        message.length(),
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                );
                analytics.setAnalytics(firebaseAnalytics, "Thank You Message", "Thank you", "Display Sign Activity Thank You");

            } else if(intent.getStringExtra(DisplaySignActivity.SIGN_NOT_FOUND) != null) {

                message = intent.getStringExtra(DisplaySignActivity.SIGN_NOT_FOUND);
                spannableStringBuilder = new SpannableStringBuilder(message);
                spannableStringBuilder.setSpan(
                        new ForegroundColorSpan(Color.RED),
                        0,
                        message.length(),
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                );
                analytics.setAnalytics(firebaseAnalytics, "Sign Not Found", "Sign Not Found", "Sign Not Found");

            } else {
                error();
                analytics.setAnalytics(firebaseAnalytics, "Error", "Error", "Error");
            }

            thankyou.setText(spannableStringBuilder);
            analytics.setAnalytics(firebaseAnalytics, "Message", "Message", "Message Displayed");
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

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void findSingButton(View view) {
        Intent findSignIntent = new Intent(this, DisplaySignActivity.class);

        if(from == null || destination == null) {
            SpannableStringBuilder spannableStringBuilder =  new SpannableStringBuilder(getResources().getString(R.string.noValidDirections));
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

    // Uncomment to test crashlytics
//    public void forceCrash(View view) {
//        throw new RuntimeException("This is a crash");
//    }

}
