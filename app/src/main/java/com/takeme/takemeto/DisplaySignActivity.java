package com.takeme.takemeto;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.takeme.takemeto.impl.Analytics;
import com.takeme.takemeto.model.Sign;
import com.takeme.takemeto.module.GlideApp;

import java.util.HashMap;

public class DisplaySignActivity extends AppCompatActivity {
    public static final String THANKYOU = "com.takeme.takemeto.THANKYOU";
    public static final String SIGN_NOT_FOUND = "com.takeme.takemeto.SIGN_NOT_FOUND";
    public static final String SIGN_COULD_NOT_BE_SHARED = "com.takeme.takemeto.SIGN_COULD_NOT_BE_SHARED";
    DatabaseReference databaseReference;
    FirebaseDatabase database;
    AdView mAdView;
    HashMap fm;
    HashMap dest;
    ProgressBar simpleProgressBar;
    TextView price;

    private FirebaseAnalytics firebaseAnalytics;
    private Analytics analytics;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_sign);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        analytics = new Analytics();
        analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Open", "DisplaySignActivity Open", "DisplaySignActivity Open");
        loadAdView();

        simpleProgressBar = (ProgressBar) findViewById(R.id.simpleProgressBar);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        final String from = intent.getStringExtra(MainActivity.FROM);
        final String destination = intent.getStringExtra(MainActivity.DESTINATION);
        price = (TextView)findViewById(R.id.price);

        database = FirebaseDatabase.getInstance();

        databaseReference = database.getReference(BuildConfig.DB);

        simpleProgressBar.setVisibility(View.VISIBLE);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Sign sign = null;


                analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity", "DisplaySignActivity Get Directions");
                // Get Image
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sign = getSignForHigherSDKVersion(from, destination, dataSnapshot);
                } else {
                    sign = getSign(from, destination, dataSnapshot);
                }

                // Display Image
                displaySign(from, destination, sign);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        TextView directionSign = findViewById(R.id.directions_sign);
        directionSign.setText(message);
    }

    private void displaySign(String from, String destination, Sign sign) {

        ImageView imageView = (ImageView) findViewById(R.id.imageView);

        if (sign != null) if (sign.getDownloadUrl() != null) {
            analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity", "Sign Found");
            if(sign.getPrice() != null) {
                price.setText("R " + sign.getPrice());
            }
            try {
                GlideApp.with(imageView.getContext()).load(sign.getDownloadUrl()).into(imageView);
                simpleProgressBar.setVisibility(View.GONE);
            } catch (Exception ise) {
                error();
            }

        } else {
            analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity",
                    "SIGN WITH NO IMAGE, from " + from.toUpperCase() + " destination " + destination.toUpperCase());
            signWithNoImage();
        }
        else {
            analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity",
                    SIGN_NOT_FOUND + " from " + from.toUpperCase() + " to " + destination.toUpperCase());
            signNotFound();
        }
    }

    private Sign getSign(String from, String destination, DataSnapshot dataSnapshot) {
        Sign sign = new Sign();

        try {
            for (DataSnapshot d : dataSnapshot.child(destination.toUpperCase()).getChildren()) {

                fm = (HashMap)d.child("from").getValue();
                dest = (HashMap)d.child("destination").getValue();
                String downloadUrl = d.child("downloadUrl").getValue(String.class);
                String priceValue = d.child("price").getValue(String.class);

                if (from.equalsIgnoreCase(fm.get("name").toString())) {
                    sign = new Sign();
                    sign.setDownloadUrl(downloadUrl);
                    sign.setPrice(priceValue);
                }
            }
            return sign;

        } catch (ClassCastException cast) {
            analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions ClassCastException", "DisplaySignActivity Get Directions",
                    cast.getMessage());
        }
        return null;
    }

    private Sign getSignForHigherSDKVersion(String from, String destination, DataSnapshot dataSnapshot) {
        Sign sign = new Sign();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dataSnapshot.child(destination.toUpperCase()).getChildren().forEach(d -> {
                    fm = (HashMap) d.child("from").getValue();
                    dest = (HashMap) d.child("destination").getValue();
                    String downloadUrl = d.child("downloadUrl").getValue(String.class);
                    String priceValue = d.child("price").getValue(String.class);

                    if (from.toUpperCase().equalsIgnoreCase(fm.get("name").toString())) {
                        sign.setDownloadUrl(downloadUrl);
                        sign.setPrice(priceValue);
                    }
                    analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Directions Found", "DisplaySignActivity", "DisplaySignActivity Directions Found");
                });
            }
            return sign;

        } catch (ClassCastException cast) {
            analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions ClassCastException", "DisplaySignActivity Get Directions",
                    cast.getMessage());
        }
        return null;
    }

    private void loadAdView() {

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adDisplayScreen);
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mAdView.loadAd(adRequest);
        analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Load Add", "DisplaySignActivity",
                "Add Displayed");

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public void gotit(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        String message = getResources().getString(R.string.signFound);
        intent.putExtra(THANKYOU, message);
        analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity",
                "Got it, happy");
        startActivity(intent);
    }

    private void signNotFound() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(SIGN_NOT_FOUND, getResources().getString(R.string.signNotFound));
        analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity",
                getResources().getString(R.string.signNotFound));
        startActivity(intent);
    }

    private void signWithNoImage() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(SIGN_NOT_FOUND, getResources().getString(R.string.signWithNoImage));
        analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity",
                getResources().getString(R.string.signWithNoImage));
        startActivity(intent);
    }

    private void error() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(SIGN_NOT_FOUND, getResources().getString(R.string.genericFailure));
        analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity",
                getResources().getString(R.string.genericFailure));
        startActivity(intent);
    }
}
