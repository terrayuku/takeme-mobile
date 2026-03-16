package com.takeme.takemeto.model;

import android.app.Application;
import com.google.android.libraries.places.api.Places;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.database.FirebaseDatabase;
import com.takeme.takemeto.BuildConfig;
import com.takeme.takemeto.R;

public class TakeMeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Enable disk persistence for faster subsequent reads
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        if (!Places.isInitialized()) {
            String key = getString(com.takeme.takemeto.R.string.maps_key).trim();
            Places.initialize(getApplicationContext(), key);
        }
        if (BuildConfig.DEBUG) {
            FirebaseAppCheck.getInstance()
                    .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
        }
    }
}