package com.takeme.takemeto;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Camera;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.ComponentNameMatchers.hasShortClassName;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsNot.not;


@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class AddSignForDirectionsTest {

    private static final String PACKAGE_NAME = "com.takeme.takemeto";
    private static String TAG = "Test Add Sign";
    private String from;
    private String to;

    private File image = new File("/home/simthembile/Pictures/tea.png");

    @Rule
    public IntentsTestRule<AddSignForDirections> intentsRule =
            new IntentsTestRule<>(AddSignForDirections.class);
    @Before
    public void initSetDirections() {
        from = "lehae";
        to = "jozi";



        // loading camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, intent);

//        intending(toPackage("android.provider")).respondWith(result);
        intending(allOf(
           hasAction(Intent.ACTION_GET_CONTENT),
                hasData(Uri.parse(image.getPath()))
        )).respondWith(result);

//        intending(not(isInternal())).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    @Test
    public void validateInputs() {

        onView(withId(R.id.from))
                .perform(typeText(from), closeSoftKeyboard());

        onView(withId(R.id.destination))
                .perform(typeText(to), closeSoftKeyboard());

        onView(withId(R.id.uploadSign)).perform(click());

        takePhoto();

        onView(withId(R.id.newImage)).check(matches(CoreMatchers.notNullValue()));
    }

    public static final int waitTimeNativeApi = 6000;

    public static void await(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while sleeping");
        }
    }

    private void takePhoto() {

        boolean usePixels = false;

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiObject titleTextUI = device.findObject(new UiSelector().resourceId("com.android.camera:id/shutter_button"));

        try {
            titleTextUI.click();

            if (usePixels) {
                takePhotoForPixels(device);
            } else {
                takePhotoForSamsung(device);
            }

        } catch (UiObjectNotFoundException unofe) {
            unofe.printStackTrace();
        }


    }

    private void takePhotoForPixels(UiDevice device) {

        // close the app selector to go back to our app so we can carry on with Espresso

        await(waitTimeNativeApi);

        //TAP on the camera icon
        device.click(device.getDisplayWidth() / 2, device.getDisplayHeight() - 100);

        await(waitTimeNativeApi);

        //Get the OK button
        device.click(device.getDisplayWidth() / 2, device.getDisplayHeight() - 100);

        await(waitTimeNativeApi);

    }

    private void takePhotoForSamsung(UiDevice device) throws UiObjectNotFoundException {

        // close the app selector to go back to our app so we can carry on with Espresso
        UiObject titleTextUI = device.findObject(new UiSelector()
                .className("android.widget.TextView")
                .text("Camera")
        );

        titleTextUI.clickTopLeft();

        await(waitTimeNativeApi);

        //TAP on the camera icon
        device.click(device.getDisplayWidth() / 2, device.getDisplayHeight() - 50);

        //Get the OK button
        UiObject cameraOkUi = device.findObject(new UiSelector()
                .className("android.widget.TextView")
                .text("OK")
        );

        cameraOkUi.click();

        await(waitTimeNativeApi);

    }
}
