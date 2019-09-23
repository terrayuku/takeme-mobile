package com.takeme.takemeto;


import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SearchASign {

    @Rule
    public ActivityTestRule<LoginActivity> mActivityTestRule = new ActivityTestRule<>(LoginActivity.class);

    @Before
    public void logout() {
        FirebaseAuth.getInstance().signOut();
    }

    @Test
    public void searchASign() {
        ViewInteraction supportVectorDrawablesButton = onView(
                allOf(withId(R.id.email_button), withText("Sign in with email"),
                        childAtPosition(
                                allOf(withId(R.id.btn_holder),
                                        childAtPosition(
                                                withId(R.id.container),
                                                0)),
                                0)));
        SystemClock.sleep(1000);
        supportVectorDrawablesButton.perform(scrollTo(), click());

        ViewInteraction textInputEditText = onView(
                allOf(withId(R.id.email),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.email_layout),
                                        0),
                                0)));
        textInputEditText.perform(scrollTo(), replaceText("peshyuku@gmail.com"), closeSoftKeyboard());

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.button_next), withText("Next"),
                        childAtPosition(
                                allOf(withId(R.id.email_top_layout),
                                        childAtPosition(
                                                withClassName(is("android.widget.ScrollView")),
                                                0)),
                                2)));
        SystemClock.sleep(1000);
        appCompatButton.perform(scrollTo(), click());

//        ViewInteraction textInputEditText2 = onView(
//                allOf(withId(R.id.password),
//                        childAtPosition(
//                                childAtPosition(
//                                        withId(R.id.password_layout),
//                                        0),
//                                0)));
//        textInputEditText2.perform(scrollTo(), replaceText(""), closeSoftKeyboard());

        ViewInteraction textInputEditText3 = onView(
                allOf(withId(R.id.password),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.password_layout),
                                        0),
                                0)));
        SystemClock.sleep(1000);
        textInputEditText3.perform(scrollTo(), replaceText("123456789"), closeSoftKeyboard());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.button_done), withText("Sign in"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                4)));
        SystemClock.sleep(1000);
        appCompatButton2.perform(scrollTo(), click());
        SystemClock.sleep(3000);
        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.places_autocomplete_search_input),
                        childAtPosition(
                                allOf(withId(R.id.from),
                                        childAtPosition(
                                                withId(R.id.findSign),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatEditText.perform(click());

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.places_autocomplete_edit_text),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("Lehae"), closeSoftKeyboard());

        ViewInteraction relativeLayout = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.places_autocomplete_list),
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2)),
                        0),
                        isDisplayed()));
        SystemClock.sleep(2000);
        relativeLayout.perform(click());

        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(R.id.places_autocomplete_search_input),
                        childAtPosition(
                                allOf(withId(R.id.destination),
                                        childAtPosition(
                                                withId(R.id.findSign),
                                                1)),
                                1),
                        isDisplayed()));
        SystemClock.sleep(2000);
        appCompatEditText3.perform(click());

        ViewInteraction appCompatEditText4 = onView(
                allOf(withId(R.id.places_autocomplete_edit_text),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        0),
                                1),
                        isDisplayed()));
        appCompatEditText4.perform(replaceText("Lenasia"), closeSoftKeyboard());

        ViewInteraction relativeLayout2 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.places_autocomplete_list),
                                childAtPosition(
                                        withClassName(is("android.widget.LinearLayout")),
                                        2)),
                        0),
                        isDisplayed()));
        SystemClock.sleep(2000);
        relativeLayout2.perform(click());

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.findDirections),
                        childAtPosition(
                                allOf(withId(R.id.findSign),
                                        childAtPosition(
                                                withClassName(is("androidx.coordinatorlayout.widget.CoordinatorLayout")),
                                                1)),
                                2),
                        isDisplayed()));
        SystemClock.sleep(2000);
        floatingActionButton.perform(click());

        ViewInteraction floatingActionButton2 = onView(
                allOf(withId(R.id.gotit),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                3),
                        isDisplayed()));
        SystemClock.sleep(3000);
        floatingActionButton2.perform(click());
    }

    @After
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
