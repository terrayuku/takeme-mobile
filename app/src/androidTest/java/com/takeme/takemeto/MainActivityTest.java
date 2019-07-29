package com.takeme.takemeto;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.ComponentNameMatchers.hasShortClassName;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class MainActivityTest {
    private String from;
    private String to;

    private static final String PACKAGE_NAME = "com.takeme.takemeto";

    @Rule
    public IntentsTestRule<MainActivity> intentsRule =
            new IntentsTestRule<>(MainActivity.class);
    @Before
    public void initSetDirections() {
        from = "lehae";
        to = "jozi";
    }

    @Test
    public void validateInputs() {

        onView(withId(R.id.from))
                .perform(typeText(from), closeSoftKeyboard());

        onView(withId(R.id.destination))
                .perform(typeText(to), closeSoftKeyboard());

        onView(withId(R.id.findDirections)).perform(click());

        intended(allOf(
                hasComponent(hasShortClassName(".DisplaySignActivity")),
                toPackage(PACKAGE_NAME),
                hasExtra(MainActivity.FROM, from),
                hasExtra(MainActivity.DESTINATION, to)
        ));
    }
}
