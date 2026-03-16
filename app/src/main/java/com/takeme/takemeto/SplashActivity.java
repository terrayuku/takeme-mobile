package com.takeme.takemeto;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private final int SPLASH_DISPLAY_DURATION = 1000;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent target;
            if (BuildConfig.DEBUG) {
                // Skip login in debug builds
                target = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                target = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(target);
            finish();
        }, SPLASH_DISPLAY_DURATION);
    }
}
