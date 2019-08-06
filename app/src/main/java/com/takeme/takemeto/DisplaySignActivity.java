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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_sign);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadAdView();

        simpleProgressBar = (ProgressBar) findViewById(R.id.simpleProgressBar);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        final String from = intent.getStringExtra(MainActivity.FROM).toUpperCase();
        final String destination = intent.getStringExtra(MainActivity.DESTINATION).toUpperCase();

        database = FirebaseDatabase.getInstance();

        databaseReference = database.getReference("signs");

        simpleProgressBar.setVisibility(View.VISIBLE);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Sign sign = null;

                // Get Image
                ImageView imageView = (ImageView) findViewById(R.id.imageView);

                for (DataSnapshot d : dataSnapshot.child(destination).getChildren()) {

                    fm = (HashMap)d.child("from").getValue();
                    dest = (HashMap)d.child("destination").getValue();
                    String downloadUrl = d.child("downloadUrl").getValue(String.class);

                    if (from.equalsIgnoreCase(fm.get("name").toString())) {
                        sign = new Sign();
                        sign.setDownloadUrl(downloadUrl);
                    }
                }

                // Display Image
                if (sign != null) if (sign.getDownloadUrl() != null) {
                    Log.i("SIGN FOUND", "SIGN FOUND");
                    GlideApp.with(imageView.getContext()).load(sign.getDownloadUrl()).into(imageView);
                } else {
                    Log.i("SIGN WITH NO IMAGE", "SIGN WITH NO IMAGE, from " + from + " destination " + destination);
                    signWithNoImage();
                }
                else {
                    Log.i("SIGN NOT FOUND", SIGN_NOT_FOUND);
                    signNotFound();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        // Capture the layout's TextView and set the string as its text
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
        startActivity(intent);
    }

    private void signNotFound() {
        Intent intent = new Intent(this, MainActivity.class);
        String message = getResources().getString(R.string.signNotFound);
        intent.putExtra(SIGN_NOT_FOUND, message);
        startActivity(intent);
    }

    private void signWithNoImage() {
        Intent intent = new Intent(this, MainActivity.class);
        String message = getResources().getString(R.string.signWithNoImage);
        intent.putExtra(SIGN_NOT_FOUND, message);
        startActivity(intent);
    }

    private void error() {
        Intent intent = new Intent(this, MainActivity.class);
        String message = getResources().getString(R.string.genericFailure);
        intent.putExtra(THANKYOU, message);
        startActivity(intent);
    }
}
