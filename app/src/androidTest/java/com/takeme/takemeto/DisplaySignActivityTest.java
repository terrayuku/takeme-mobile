package com.takeme.takemeto;

import android.view.View;
import android.widget.TextView;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

@RunWith(AndroidJUnit4ClassRunner.class)
@LargeTest
public class DisplaySignActivityTest {
    private String from;
    private String to;
    private String message;
    @Rule
    public IntentsTestRule<DisplaySignActivity> intentsTestRule = new IntentsTestRule<>(DisplaySignActivity.class);

    // overriding the set text in view function in view action to allow TextView
    public static ViewAction setTextInTextView(final String value) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(TextView.class));
            }

            @Override
            public String getDescription() {
                return "replace text";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TextView) view).setText(value);
            }
        };
    }

    @Before
    public void initSetDirections() {
        from = "lehae";
        to = "jozi";
        message = "From " + from + " To " + to;
    }

    @Test
    public void findSign() {

        onView(withId(R.id.directions_sign))
                .perform(setTextInTextView("From " + from + " To " + to));

        onView(withId(R.id.directions_sign))
                .check(matches(withText(message)));
    }

}
