package com.takeme.takemeto;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.NumberFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.takeme.takemeto.impl.Location;
import com.takeme.takemeto.model.Sign;

public class AddSignForDirections extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String THANKYOU = "com.takeme.takemeto.THANKYOU";

    String currentPhotoPath;
    String currentPhotoName;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    ImageView newImageView;
    TextView message;
    private AdView mAdView;
    Location location;

    private StorageReference reference;
    private StorageMetadata metadata;
    private UploadTask uploadTask;
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    Place from;
    Place destination;
    ProgressBar simpleProgressBar;
    public static final String TAG = "AddSignForDirections";
    private View mLayout;
    private EditText priceValue;


    private static final int REQUEST_CAMERA = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_sign_for_directions);

        mLayout = findViewById(R.id.message);
        priceValue = (EditText) findViewById(R.id.price);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadAdView();
        location = new Location();

        simpleProgressBar = (ProgressBar) findViewById(R.id.simpleProgressBar);

        newImageView = (ImageView) findViewById(R.id.newImage);
        message = (TextView) findViewById(R.id.message);
        reference = FirebaseStorage.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.maps_key));
        }

        Location location = new Location();

        AutocompleteSupportFragment fromFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.from);
        AutocompleteSupportFragment toFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.to);

        if (fromFragment != null && toFragment != null) {
            // E/Places: Error while autocompleting: TIMEOU
            location.setPlace(fromFragment, "From...").setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    from = place;
                }

                @Override
                public void onError(@NonNull Status status) {
                    message.setText(getResources().getString(R.string.location_error));
                }
            });

            location.setPlace(toFragment, "To...").setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    destination = place;
                }

                @Override
                public void onError(@NonNull Status status) {
                    message.setText(getResources().getString(R.string.location_error));
                }
            });
        }
    }

    private void requestCameraPermission() {

        // BEGIN_INCLUDE(camera_permission_request)
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            Snackbar.make(mLayout, R.string.permission_camera_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(AddSignForDirections.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA);
                        }
                    })
                    .show();
        } else {

            // Camera permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CAMERA) {
            // BEGIN_INCLUDE(permission_result)
            // Received permission result for camera permission.

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                Snackbar.make(mLayout, R.string.permision_available_camera,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(mLayout, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public void uploadSign(View view) {
        dispatchTakePictureIntent();
        galleryAddPic();
        setPic();
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                // do your stuff
            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                return;
            }
        });
    }

    public void addSign(View view) {
        if (destination != null && from != null && newImageView != null) {
            if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                uploadFile(currentPhotoPath);
            } else {
                noImageCaptured();
            }
        } else {
            invalidValues();
        }
    }

    // take the file saved when taking the picture
    public void uploadFile(String newSignPath) {

        final Uri file = Uri.fromFile(new File(newSignPath));
        Bitmap bmp = null;
        byte[] data = null;
        try {
            // Reducing image size to increase performance
            bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            data = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
//         Save the coordinates with the download url on db

        metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("from", from.getName())
                .setCustomMetadata("destination", destination.getName())
                .build();

        uploadTask = reference.child(BuildConfig.BUCKET + file.getLastPathSegment()).putBytes(data, metadata);

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                try {
                    double progress = (100 * taskSnapshot.getBytesTransferred()) / (double) taskSnapshot.getTotalByteCount();
                    message.setText(getResources().getString(R.string.uploadPrefix) + " " + Math.round(progress) + " " + getResources().getString(R.string.uploadSurfix));

                    simpleProgressBar.setVisibility(View.VISIBLE);
                } catch (ArithmeticException ae) {
                    error();
                }

            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                message.setText(getResources().getString(R.string.uploadPaused));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                message.setText(getResources().getString(R.string.uploadFailed));
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                final Sign sign = new Sign(destination, from, priceValue.getText().toString());

                if (mAuth.getCurrentUser() != null) {

                    database = FirebaseDatabase.getInstance();
                    databaseReference = database.getReference(BuildConfig.DB);
                    // download url
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            sign.setDownloadUrl(uri.toString());
                            sign.setUserUID(mAuth.getCurrentUser().getUid());

                            databaseReference.child(sign.getDestination().getName().toUpperCase()).push().setValue(sign).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    success();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    error();
                                }
                            });
                        }
                    });

                } else {
                    error();
                }
            }
        });
    }

    private void loadAdView() {

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        mAdView = findViewById(R.id.adDirectionsScreen);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mAdView.loadAd(adRequest);
    }

    private void success() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(THANKYOU, getResources().getString(R.string.uploadSuccess));
        startActivity(intent);
    }

    private void error() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(THANKYOU, getResources().getString(R.string.genericFailure));
        startActivity(intent);
    }

    private void invalidValues() {
        message.setText(getResources().getString(R.string.noValidDirections));
    }

    private void noImageCaptured() {
        message.setText(getResources().getString(R.string.noImageCaptured));
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity destination handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.takeme.takemeto.provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            } else {
                error();
            }
        } else {
            error();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Glide.with(this).load(currentPhotoPath).into(newImageView);
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        if (!image.getAbsolutePath().isEmpty()) {
            currentPhotoPath = image.getAbsolutePath();
            currentPhotoName = image.getName();
        } else {
            noImageCaptured();
        }
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = newImageView.getWidth();
        int targetH = newImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much destination scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized destination fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        newImageView.setImageBitmap(bitmap);
    }
}
