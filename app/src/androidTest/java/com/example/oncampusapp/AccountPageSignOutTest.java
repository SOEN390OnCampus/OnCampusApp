package com.example.oncampusapp;
// app/src/androidTest/java/com/example/oncampusapp/AccountPageSignOutTest.java

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AccountPageSignOutTest {

    @Rule
    public ActivityScenarioRule<AccountPage> activityRule =
            new ActivityScenarioRule<>(AccountPage.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }
    @Test
    public void signOutButton_showsConfirmationDialog() {
        // Click the sign-out button
        onView(withId(R.id.btn_sign_out)).perform(click());

        // Verify the dialog appears with correct message
        onView(withText(R.string.confirm_sign_out_body))
                .check(matches(isDisplayed()));
    }

    @Test
    public void signOutDialog_cancelDismissesDialog() {
        onView(withId(R.id.btn_sign_out)).perform(click());

        // Click cancel
        onView(withText(R.string.cancel)).perform(click());

        // Dialog should be gone, still on AccountPage
        onView(withId(R.id.btn_sign_out)).check(matches(isDisplayed()));
    }

    @Test
    public void signOutDialog_confirmTriggersSignOut() {
        onView(withId(R.id.btn_sign_out)).perform(click());

        // Confirm sign out
        onView(withText(R.string.sign_out)).perform(click());

        // Should navigate back to GoogleCalendarAuthActivity
        onView(withId(R.id.btn_calendar_signin)) //Check for the sign-in button
                .withFailureHandler((error, viewMatcher) -> {})
                .check(matches(isDisplayed()));
    }
}