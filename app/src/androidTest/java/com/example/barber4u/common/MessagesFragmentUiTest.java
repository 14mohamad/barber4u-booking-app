package com.example.barber4u.common;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MessagesFragmentUiTest {

    @Test
    public void showRatingDialog_opensDialog_andCancelCloses() {

        // Launch the fragment in the default test container
        FragmentScenario<MessagesFragment> scenario =
                FragmentScenario.launchInContainer(MessagesFragment.class);

        // Trigger the dialog from the fragment
        scenario.onFragment(fragment -> {
            MessageItem msg = new MessageItem(
                    "msg1",
                    "hello",
                    "appt1",
                    "barber1",
                    "David"
            );
            fragment.onRateNow(msg);
        });

        // Verify dialog appears
        onView(withText("Rate David"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));

        // Click Cancel
        onView(withText("Cancel"))
                .inRoot(isDialog())
                .perform(click());

        // Verify dialog is dismissed
        onView(withText("Rate David"))
                .inRoot(isDialog())
                .check(doesNotExist());
    }
}
