package com.example.opsc7312poepart2_code;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static java.util.function.Predicate.not;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.Root;
import androidx.test.espresso.idling.CountingIdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.example.poe2.MainActivity;
import com.example.poe2.R;
import com.example.poe2.ui.maps_client.MapsClientFragment;
import com.google.android.gms.maps.model.LatLng;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.CoreMatchers.not;

import android.app.Activity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

// Adapted from: Android Testing Documentation
// Source URL: https://developer.android.com/training/testing
// Contributors: Android Developers
// Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
@RunWith(AndroidJUnit4.class) // Specifies that the tests should be run with the AndroidJUnit4 test runner
public class MapsUnitTest {

    private FragmentScenario<MapsClientFragment> fragmentScenario;
    private TestNavHostController navController;
    private ActivityScenario<MainActivity> activityScenario;

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class); // Rule to launch MainActivity before each test

    @Before
    public void setup() {
        // Launch MainActivity and set up the navigation controller
        activityScenario = ActivityScenario.launch(MainActivity.class);

        activityScenario.onActivity(activity -> {
            navController = new TestNavHostController(activity);
            navController.setGraph(com.example.poe2.R.navigation.mobile_navigation); // Set the navigation graph
            navController.setCurrentDestination(com.example.poe2.R.id.nav_maps_client); // Set the current destination
        });

        // Launch the MapsClientFragment in a container
        fragmentScenario = FragmentScenario.launchInContainer(MapsClientFragment.class);

        fragmentScenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController); // Set the navigation controller for the fragment
        });
    }

    @Test
    public void testDirectionsTextDisplayed() {
        fragmentScenario.onFragment(fragment -> {
            // Simulate setting directions text
            String simulatedDirections = "Estimated travel time: 30 mins";
            fragment.getTextViewDirection().setText(simulatedDirections);
        });

        // Check if the directions text is displayed correctly
        onView(withId(com.example.poe2.R.id.textViewDirection))
                .check(matches(withText("Estimated travel time: 30 mins")));
    }

    @Test
    public void testLocationPermissionsRequested() {
        final CountingIdlingResource idlingResource = new CountingIdlingResource("LocationPermission");
        IdlingRegistry.getInstance().register(idlingResource);
        idlingResource.increment();

        fragmentScenario.onFragment(fragment -> {
            activityScenario.onActivity(activity -> {
                activity.runOnUiThread(() -> {
                    // Request location permissions
                    fragment.requestLocationPermissions();
                });
            });
        });

        try {
            Thread.sleep(5000); // Wait for the permission request to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        idlingResource.decrement(); // Decrement the idling resource

        // Check if the directions text view is displayed
        onView(withId(com.example.poe2.R.id.textViewDirection))
                .check(matches(isDisplayed()));

        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    @Test
    public void testDentistSpinnerLoad() {
        try {
            Thread.sleep(5000); // Wait for the spinner to load
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fragmentScenario.onFragment(fragment -> {
            // Check if the dentist spinner is not null and has items
            assertNotNull(fragment.getSpinnerDentists());
            assertTrue(fragment.getSpinnerDentists().getAdapter().getCount() > 0);
        });
    }
}

