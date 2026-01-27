package com.example.barber4u.customer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.os.IBinder;
import android.view.WindowManager;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.barber4u.R;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BranchSelectionFragmentUiTest {

    @Before
    public void launchFragment() {
        FragmentScenario.launchInContainer(BranchSelectionFragment.class);
    }

    @Test
    public void selectButton_isDisabledInitially() {
        // btnSelectBranch is setEnabled(false) in onViewCreated
        onView(withId(R.id.btnSelectBranch)).check(matches(isNotEnabled()));
    }

    @Test
    public void clickingSelect_withoutChoosingBranch_showsToast() {
        // We want to click the button, but it is disabled by default.
        // So this test validates the "Choose a branch" behavior by enabling button
        // (UI tests can do controlled setup via scenario.onFragment).
        FragmentScenario<BranchSelectionFragment> scenario =
                FragmentScenario.launchInContainer(BranchSelectionFragment.class);

        scenario.onFragment(fragment -> {
            // enable button so we can click it; keep selectedBranch == null
            fragment.requireView().findViewById(R.id.btnSelectBranch).setEnabled(true);
        });

        onView(withId(R.id.btnSelectBranch)).perform(click());

        // Toast text from returnSelectedBranch(): "Choose a branch"
        onView(withText("Choose a branch"))
                .inRoot(new ToastMatcher())
                .check(matches(withText("Choose a branch")));
    }

    // -------- Toast matcher (standard Espresso pattern) --------
    public static class ToastMatcher extends TypeSafeMatcher<androidx.test.espresso.Root> {
        @Override
        public void describeTo(Description description) {
            description.appendText("is toast");
        }

        @Override
        public boolean matchesSafely(androidx.test.espresso.Root root) {
            int type = root.getWindowLayoutParams().get().type;
            if ((type == WindowManager.LayoutParams.TYPE_TOAST)
                    || (type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    || (type == WindowManager.LayoutParams.TYPE_APPLICATION_PANEL)) {
                IBinder windowToken = root.getDecorView().getWindowToken();
                IBinder appToken = root.getDecorView().getApplicationWindowToken();
                return windowToken == appToken;
            }
            return false;
        }
    }
}
