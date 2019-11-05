package com.takeme.takemeto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private static final int RC_SIGN_IN = 343;
    Button login;
    AdView mAdView;
    View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.home);

        auth = FirebaseAuth.getInstance();
        login = (Button) findViewById(R.id.login);
        mLayout = (TextView) findViewById(R.id.snackbar_text);

        EditText email = (EditText) findViewById(R.id.email);
        EditText password = (EditText) findViewById(R.id.password);


        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (email != null && password != null) {
                    signIn(email.getText().toString().trim(), password.getText().toString().trim());
                } else {
                    Snackbar.make(mLayout, "Authentication failed.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        loadAdView();
    }

    private void loadAuthUI() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder()
                        .setDefaultCountryIso("ZA")
                        .build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
//                        .setLogo(R.mipmap.taxi_layer)
                        .build(),
                RC_SIGN_IN);
    }

    private void signIn(String email, String password) {

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            loadMainActivity();
                        } else {
                            Snackbar.make(mLayout, task.getException().getMessage(),
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                loadMainActivity();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Already signed in
            loadMainActivity();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        moveTaskToBack(true);
    }

    private void loadMainActivity() {
        Intent findSignIntent = new Intent(this, MainActivity.class);
        startActivity(findSignIntent);
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
}
