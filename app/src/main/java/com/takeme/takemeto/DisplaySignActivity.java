package com.takeme.takemeto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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
    private static final String TAG = "DisplaySign";
    public static final String THANKYOU = "com.takeme.takemeto.THANKYOU";
    public static final String SIGN_NOT_FOUND = "com.takeme.takemeto.SIGN_NOT_FOUND";
    public static final String SIGN_COULD_NOT_BE_SHARED = "com.takeme.takemeto.SIGN_COULD_NOT_BE_SHARED";
    DatabaseReference databaseReference;
    FirebaseDatabase database;
    AdView mAdView;
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

        // Signs are stored under signs/{DESTINATION_NAME_UPPERCASE}/{pushKey}
        String destKey = destination != null ? destination.toUpperCase() : "";

        String dbPath = BuildConfig.DB + "/" + destKey;
        Log.d(TAG, "from='" + from + "' destination='" + destination + "'");
        Log.d(TAG, "Querying Firebase path: " + dbPath);

        databaseReference.child(destKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot destinationSnapshot) {
                Log.d(TAG, "onDataChange: exists=" + destinationSnapshot.exists()
                        + " childrenCount=" + destinationSnapshot.getChildrenCount()
                        + " key=" + destinationSnapshot.getKey());

                Sign sign = findSign(from, destinationSnapshot);
                Log.d(TAG, "findSign result: " + (sign != null ? "FOUND url=" + sign.getDownloadUrl() : "NOT FOUND"));
                displaySign(from, destination, sign);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled: " + databaseError.getMessage());
                simpleProgressBar.setVisibility(View.GONE);
                signNotFound();
            }
        });

        // Timeout — if Firebase hasn't responded in 8 seconds, show not found
        simpleProgressBar.postDelayed(() -> {
            if (simpleProgressBar.getVisibility() == View.VISIBLE) {
                simpleProgressBar.setVisibility(View.GONE);
                signNotFound();
            }
        }, 8000);
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
                simpleProgressBar.setVisibility(View.VISIBLE);
                GlideApp.with(imageView.getContext())
                        .load(sign.getDownloadUrl())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                    Target<Drawable> target, boolean isFirstResource) {
                                simpleProgressBar.setVisibility(View.GONE);
                                error();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model,
                                    Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                simpleProgressBar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        .into(imageView);
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

    /**
     * Searches entries under the destination node for one matching the given 'from' name.
     * DB structure: signs/{DEST_UPPERCASE}/{pushKey}/from/name
     */
    private Sign findSign(String from, DataSnapshot destinationSnapshot) {
        try {
            for (DataSnapshot entry : destinationSnapshot.getChildren()) {
                String fromName = entry.child("from").child("name").getValue(String.class);
                Log.d(TAG, "  entry=" + entry.getKey() + " from/name='" + fromName + "' looking for='" + from + "'");
                if (fromName != null && from.equalsIgnoreCase(fromName)) {
                    Sign sign = new Sign();
                    sign.setDownloadUrl(entry.child("downloadUrl").getValue(String.class));
                    sign.setPrice(entry.child("price").getValue(String.class));
                    return sign;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "findSign error", e);
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
                .addKeyword(AdRequest.DEVICE_ID_EMULATOR)
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
