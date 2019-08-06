package com.takeme.takemeto;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
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

public class AddSignForDirections extends AppCompatActivity {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_sign_for_directions);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadAdView();
        location = new Location();

        simpleProgressBar = (ProgressBar) findViewById(R.id.simpleProgressBar);

        newImageView = (ImageView) findViewById(R.id.newImage);
        message = (TextView) findViewById(R.id.message);
        reference = FirebaseStorage.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();
        signInAnonymously();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.maps_key));
        }

        AutocompleteSupportFragment fromFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.from);
        AutocompleteSupportFragment toFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.to);

        if (fromFragment != null) {
            fromFragment.setHint("From...");
            fromFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));
            fromFragment.setLocationBias(RectangularBounds.newInstance(
                    new LatLng(-34.277857,18.2359139),
                    new LatLng(-23.9116035,29.380895)));

            fromFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    from = place;
                }

                @Override
                public void onError(@NonNull Status status) {

                }
            });
        }

        if (toFragment != null) {
            toFragment.setHint("To...");
            toFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));
            toFragment.setLocationBias(RectangularBounds.newInstance(
                    new LatLng(-34.277857,18.2359139),
                    new LatLng(-23.9116035,29.380895)));

            toFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(Place place) {
                    destination = place;
                }

                @Override
                public void onError(@NonNull Status status) {

                }
            });
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
                Log.e("SIGNIN", "signInAnonymously:FAILURE", exception);
            }
        });
    }

    public void addSign(View view) {
        if(destination != null && from != null && newImageView != null) {
            if(currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
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
//         Save the coordinates with the download url on db

        metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("from", from.getName())
                .setCustomMetadata("destination", destination.getName())
                .build();

        Log.i("Reference", reference.getName());
        uploadTask = reference.child("images/" + file.getLastPathSegment()).putFile(file, metadata);

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                message.setText(getResources().getString(R.string.uploadPrefix) + " " + Math.round(progress) + " " + getResources().getString(R.string.uploadSurfix));

                simpleProgressBar.setVisibility(View.VISIBLE);

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

                final Sign sign = new Sign(destination, from);

                if (mAuth.getCurrentUser().isAnonymous()) {

                    database = FirebaseDatabase.getInstance();
                    databaseReference = database.getReference("signs");
                    // download url
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            sign.setDownloadUrl(uri.toString());

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
                    Log.i("ERROR SAVING SIGN", mAuth.getCurrentUser().toString());
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
                Log.i("PhotoUri", photoURI.getPath());
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
        if(!image.getAbsolutePath().isEmpty()) {
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
