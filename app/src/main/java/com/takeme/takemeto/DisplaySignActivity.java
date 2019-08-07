package com.takeme.takemeto;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    public static final String SIGN_NOT_FOUND = "com.takeme.takemeto.SOGN_NOT_FOUND";
    public static final String SIGN_COULD_NOT_BE_SHARED = "com.takeme.takemeto.SIGN_COULD_NOT_BE_SHARED";
    private DatabaseReference databaseReference;
    private FirebaseDatabase database;
    private AdView mAdView;
    HashMap fm;
    HashMap dest;
    ProgressBar simpleProgressBar;

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

        final String from = intent.getStringExtra(MainActivity.FROM).toUpperCase();
        final String destination = intent.getStringExtra(MainActivity.DESTINATION).toUpperCase();

        database = FirebaseDatabase.getInstance();

        databaseReference = database.getReference(BuildConfig.DB);

        simpleProgressBar.setVisibility(View.VISIBLE);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Sign sign = null;

                // Get Image
                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity", "DisplaySignActivity Get Directions");
                for (DataSnapshot d : dataSnapshot.child(destination).getChildren()) {
                    try {
                        fm = (HashMap)d.child("from").getValue();
                        dest = (HashMap)d.child("destination").getValue();
                        String downloadUrl = d.child("downloadUrl").getValue(String.class);

                        if (from.equalsIgnoreCase(fm.get("name").toString())) {
                            sign = new Sign();
                            sign.setDownloadUrl(downloadUrl);
                        }
                        analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Directions Found", "DisplaySignActivity", "DisplaySignActivity Directions Found");
                    } catch(ClassCastException cast) {
                        analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions ClassCastException", "DisplaySignActivity Get Directions",
                                cast.getMessage());
                    }

                }

                // Display Image
                if (sign != null) if (sign.getDownloadUrl() != null) {
                    analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity", "Sign Found");
                    GlideApp.with(imageView.getContext()).load(sign.getDownloadUrl()).into(imageView);
                } else {
                    analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity",
                            "SIGN WITH NO IMAGE, from " + from + " destination " + destination);
                    signWithNoImage();
                }
                else {
                    analytics.setAnalytics(firebaseAnalytics, "DisplaySignActivity Get Directions", "DisplaySignActivity",
                            SIGN_NOT_FOUND + " from " + from + " to " + destination);
                    signNotFound();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        TextView directionSign = findViewById(R.id.directions_sign);
        directionSign.setText(message);
    }

    private void loadAdView() {

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adDisplayScreen);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
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
