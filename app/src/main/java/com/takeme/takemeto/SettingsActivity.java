package com.takeme.takemeto;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            EditTextPreference displayName = (EditTextPreference) findPreference("display_name");
            EditTextPreference phoneNumber = (EditTextPreference) findPreference("phone_number");

            Preference terms = (Preference) findPreference("terms");
            Preference policy = (Preference) findPreference("policy");

            if (user != null && displayName != null) {

                displayName.setText(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

            }

            if (user != null && phoneNumber != null) {

                phoneNumber.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

            }
            if (displayName != null || !displayName.getText().equalsIgnoreCase("Not Set")) {
                displayName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        displayName.setText(newValue.toString());
                        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                                .setDisplayName(newValue.toString())
                                .build();

                        user.updateProfile(profileChangeRequest).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    return;
                                }
                            }
                        });

                        return false;
                    }
                });

            }

            if (phoneNumber != null) {
                phoneNumber.setEnabled(false);
            }

            if(terms != null) {
                terms.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://takemeto.co.za/#/termsandconditions")));
                        return false;
                    }
                });
            }

            if(policy != null) {
                policy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://takemeto.co.za/#/privacy/policy")));
                        return false;
                    }
                });
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}