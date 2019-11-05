package com.takeme.takemeto;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private final int SPLASH_DISPLAY_DURATION = 1000;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {

                Intent mainIntent = new Intent(SplashActivity.this, LoginActivity.class);
                SplashActivity.this.startActivity(mainIntent);
                SplashActivity.this.finish();
            }
        }, SPLASH_DISPLAY_DURATION);
    }
}
