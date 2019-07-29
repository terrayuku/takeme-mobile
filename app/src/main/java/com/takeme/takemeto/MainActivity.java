package com.takeme.takemeto;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.takeme.takemeto.MESSAGE";
    public static final String FROM = "com.takeme.takemeto.FROM";
    public static final String DESTINATION = "com.takeme.takemeto.DESTINATION";
    TextView thankyou;
    EditText from;
    EditText destination;
    FloatingActionButton findDirections;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadAdView();

        from = (EditText)findViewById(R.id.from);
        destination = (EditText)findViewById(R.id.destination);
        findDirections = (FloatingActionButton) findViewById(R.id.findDirections);


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        thankyou = (TextView)findViewById(R.id.thankyou);
        Intent intent = getIntent();
        displayMessage(intent);

        final Intent addSingIntent = new Intent(this, AddSignForDirections.class);

        FloatingActionButton fab = findViewById(R.id.addDirections);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(addSingIntent);
            }
        });
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

    private void displayMessage(Intent intent) {
        if(intent != null) {
            String message = "";
            SpannableStringBuilder spannableStringBuilder = null; // = new SpannableStringBuilder(message);
            if(intent.getStringExtra(DisplaySignActivity.THANKYOU) != null) {

                message = intent.getStringExtra(DisplaySignActivity.THANKYOU);
                spannableStringBuilder = new SpannableStringBuilder(message);
                spannableStringBuilder.setSpan(
                        new ForegroundColorSpan(Color.BLACK),
                        0,
                        message.length(),
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                );

            } else if(intent.getStringExtra(DisplaySignActivity.SIGN_NOT_FOUND) != null) {

                message = intent.getStringExtra(DisplaySignActivity.SIGN_NOT_FOUND);
                spannableStringBuilder = new SpannableStringBuilder(message);
                spannableStringBuilder.setSpan(
                        new ForegroundColorSpan(Color.RED),
                        0,
                        message.length(),
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                );

            } else if(intent.getStringExtra(DisplaySignActivity.SIGN_COULD_NOT_BE_SHARED) != null) {

                message = intent.getStringExtra(DisplaySignActivity.SIGN_COULD_NOT_BE_SHARED);
                spannableStringBuilder = new SpannableStringBuilder(message);
                spannableStringBuilder.setSpan(
                        new ForegroundColorSpan(Color.RED),
                        0,
                        message.length(),
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                );

            }

            thankyou.setText(spannableStringBuilder);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void findSingButton(View view) {
        Intent findSignIntent = new Intent(this, DisplaySignActivity.class);
        // disable direction sign button
        if(from.getText().toString().isEmpty() & destination.getText().toString().isEmpty()) {
            SpannableStringBuilder spannableStringBuilder =  new SpannableStringBuilder("Please enter valid directions");
            spannableStringBuilder.setSpan(
                    new ForegroundColorSpan(Color.RED),
                    0,
                    "Please enter valid directions".length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
            );
            thankyou.setText(spannableStringBuilder);
        } else {
            String message = "From " + from.getText().toString() + " To " + destination.getText().toString();
            findSignIntent.putExtra(EXTRA_MESSAGE, message);
            findSignIntent.putExtra(DESTINATION, destination.getText().toString());
            findSignIntent.putExtra(FROM, from.getText().toString());
            startActivity(findSignIntent);
        }

    }
}
