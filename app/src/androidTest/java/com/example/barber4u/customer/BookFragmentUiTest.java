package com.example.barber4u.customer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;


import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.barber4u.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.PickerActions;

@RunWith(AndroidJUnit4.class)
public class BookFragmentUiTest {

    @Before
    public void launchFragment() {
        // Launch fragment in isolation (no Activity needed)
        FragmentScenario.launchInContainer(BookFragment.class);
    }

    @Test
    public void dateAndTimeSelection_updatesFields() {
        // Click date field → pick a date → confirm
        onView(withId(R.id.etDate)).perform(click());
        onView(withClassName(org.hamcrest.Matchers.equalTo("android.widget.DatePicker")))
                .perform(PickerActions.setDate(2026, 2, 1));
        onView(withText("OK")).perform(click());

        // Click time field → pick a time → confirm
        onView(withId(R.id.etTime)).perform(click());
        onView(withClassName(org.hamcrest.Matchers.equalTo("android.widget.TimePicker")))
                .perform(PickerActions.setTime(10, 30));
        onView(withText("OK")).perform(click());

        // Verify fields are not empty
        onView(withId(R.id.etDate)).check(matches(not(withText(""))));
        onView(withId(R.id.etTime)).check(matches(not(withText(""))));
    }

    @Test
    public void bookButton_remainsDisabled_withoutBranchAndBarber() {
        // Initially disabled
        onView(withId(R.id.btnBook)).check(matches(isNotEnabled()));

        // Select date
        onView(withId(R.id.etDate)).perform(click());
        onView(withClassName(org.hamcrest.Matchers.equalTo("android.widget.DatePicker")))
                .perform(PickerActions.setDate(2026, 2, 1));
        onView(withText("OK")).perform(click());

        // Select time
        onView(withId(R.id.etTime)).perform(click());
        onView(withClassName(org.hamcrest.Matchers.equalTo("android.widget.TimePicker")))
                .perform(PickerActions.setTime(10, 30));
        onView(withText("OK")).perform(click());

        // Still disabled because branch + barber not chosen
        onView(withId(R.id.btnBook)).check(matches(isNotEnabled()));
    }
}
