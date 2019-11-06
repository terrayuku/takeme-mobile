package com.takeme.takemeto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import io.fabric.sdk.android.Fabric;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private static final int RC_SIGN_IN = 343;
    private ProgressBar progressBar;
    private boolean isLogin = true;
    private boolean isFPassword = false;
    private TextView signUp, forgotPassword;
    private EditText etPassword, etEmail, name, surname;
    Button login;
    AdView mAdView;
    View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.home);

        auth = FirebaseAuth.getInstance();
        login = findViewById(R.id.login);
        mLayout = findViewById(R.id.snackbar_text);
        progressBar = findViewById(R.id.progress_bar_login);
        signUp = findViewById(R.id.sign_up_text_login);
        forgotPassword = findViewById(R.id.forgot_password_login);
        etEmail = findViewById(R.id.email);
        etPassword = findViewById(R.id.password);
        name = findViewById(R.id.name);
        surname = findViewById(R.id.surname);

        name.setVisibility(View.GONE);
        surname.setVisibility(View.GONE);

        forgotPassword.setOnClickListener(v -> {
            isFPassword = true;
            etPassword.setVisibility(View.GONE);
            login.setText("Reset password");
            forgotPassword.setVisibility(View.GONE);
            signUp.setText("Go to Login");
        });

        signUp.setOnClickListener(v -> {
            if (isFPassword){
                isFPassword = false;
                etPassword.setVisibility(View.VISIBLE);
                login.setText("Login");

                name.setVisibility(View.GONE);
                surname.setVisibility(View.GONE);

                forgotPassword.setVisibility(View.VISIBLE);
                signUp.setText(getResources().getString(R.string.create_new_account));
            }else {
                if (isLogin) {
                    isLogin = false;
                    forgotPassword.setVisibility(View.GONE);

                    name.setVisibility(View.VISIBLE);
                    surname.setVisibility(View.VISIBLE);

                    login.setText("Sign Up");
                    signUp.setText("Already have an account? Sign In!");
                } else {
                    isLogin = true;
                    login.setText("Login");
                    signUp.setText(getResources().getString(R.string.create_new_account));

                    name.setVisibility(View.GONE);
                    surname.setVisibility(View.GONE);

                    forgotPassword.setVisibility(View.VISIBLE);
                }
            }
        });

        login.setOnClickListener(view -> {
            String mEmail = etEmail.getText().toString().trim();
            String mPass = etPassword.getText().toString().trim();
            if (isFPassword) {
                resetPassword(mEmail);
            } else {
                if (isLogin)
                    signIn(mEmail, mPass);
                else
                    signUp(mEmail, mPass,
                            name.getText().toString().trim(),
                            surname.getText().toString().trim());
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

        if (email == null || email.isEmpty()) {
            Snackbar snackbar = Snackbar.make(mLayout, "Enter Email address", Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        if (password == null || password.isEmpty()) {
            Snackbar snackbar = Snackbar.make(mLayout, "Enter correct password", Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        loadMainActivity();
                    } else {
                        Snackbar.make(mLayout, task.getException().getMessage(),
                                Snackbar.LENGTH_SHORT).show();
                    }
                });

    }

    private void resetPassword(String email){
        if (email.isEmpty()){
            Snackbar snackbar = Snackbar.make(mLayout, "Please enter your Email", Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }


        if (isValidEmailId(email)) {
            Snackbar snackbar = Snackbar.make(mLayout, "InValid Email address", Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    isFPassword = false;
                    etPassword.setVisibility(View.VISIBLE);
                    login.setText("Login");
                    forgotPassword.setVisibility(View.VISIBLE);
                    signUp.setVisibility(View.VISIBLE);
                }).addOnFailureListener(e -> {
            Snackbar snackbar = Snackbar.make(mLayout, "Failed to reset password, please try again!", Snackbar.LENGTH_LONG);
            snackbar.show();
        });
    }

    private void signUp(String email, String password, String name, String surname) {
        if (TextUtils.isEmpty(email)) {
            Snackbar snackbar = Snackbar.make(mLayout, "Enter Email address", Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Snackbar snackbar = Snackbar.make(mLayout, "Enter  password",
                    Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        if (password.length() < 6) {
            Snackbar snackbar = Snackbar.make(mLayout, "Password too short,enter 6 minimum characters",
                    Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        if (isValidEmailId(email)) {
            Snackbar snackbar = Snackbar.make(mLayout, "InValid Email address", Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        //create user
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful()) {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Snackbar snackbar = Snackbar.make(mLayout,
                                    "User with this  Email address, already exists", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    } else {
                        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name + " " + surname)
                                .build();

                        auth.getCurrentUser().updateProfile(profileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Snackbar snackbar = Snackbar.make(mLayout,
                                            "User Created With " + email, Snackbar.LENGTH_LONG);
                                    snackbar.show();
                                    loadMainActivity();
                                }
                            }
                        });

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

        MobileAds.initialize(this, initializationStatus -> {
        });

        mAdView = findViewById(R.id.adMain);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mAdView.loadAd(adRequest);
    }

    private boolean isValidEmailId(String email) {
        return !Pattern.compile("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$").matcher(email).matches();
    }
}
