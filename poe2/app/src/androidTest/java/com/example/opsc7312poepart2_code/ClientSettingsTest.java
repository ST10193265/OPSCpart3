package com.example.opsc7312poepart2_code;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.example.poe2.MainActivity;
import com.example.poe2.R;
import com.example.poe2.ui.client_settings.ClientSettingsFragment;

// Adapted from: Android Testing Documentation
// Source URL: https://developer.android.com/training/testing
// Contributors: Android Developers
// Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
@RunWith(AndroidJUnit4.class)  // Specifies that this class will use the AndroidJUnit4 test runner
public class ClientSettingsTest {

    private FragmentScenario<ClientSettingsFragment> fragmentScenario;  // Handles the lifecycle of the fragment in a testable way
    private TestNavHostController navController;  // Mock navigation controller for testing fragment navigation

    @Before
    public void setup() {
        // Launches the MainActivity in the test environment
        ActivityScenario<MainActivity> activityScenario = ActivityScenario.launch(MainActivity.class);

        activityScenario.onActivity(activity -> {
            // Sets up a TestNavHostController with the app's navigation graph
            navController = new TestNavHostController(activity);
            navController.setGraph(R.navigation.mobile_navigation);
            navController.setCurrentDestination(R.id.nav_settings_client);  // Sets the current destination to the Client Settings fragment
        });

        // Launches the ClientSettingsFragment in isolation for testing
        fragmentScenario = FragmentScenario.launchInContainer(ClientSettingsFragment.class);

        fragmentScenario.onFragment(fragment -> {
            // Connects the fragment to the mock NavController
            Navigation.setViewNavController(fragment.requireView(), navController);

            // Sets up the fragment's input fields with test data
            fragment.getEtEmail().setText("test@example.com");
            fragment.getEtPhone().setText("1234567890");
            fragment.getSpinnerLanguage().setSelection(1); // Select Afrikaans in language spinner
            fragment.getSpinnerDistanceUnits().setSelection(0); // Select km in distance units spinner
            fragment.getSpinnerDistanceRadius().setSelection(0); // Select 'No Limit' in distance radius spinner
        });
    }

    @Test
    public void testSaveButton() {
        // Simulates a click on the Save button
        onView(withId(R.id.btnSave)).perform(click());

        // Verifies that after clicking save, the NavController stays on the Client Settings destination
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_settings_client;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
        });
    }

    @Test
    public void testCancelButton() {
        // Simulates a click on the Cancel button
        onView(withId(R.id.btnCancel)).perform(click());

        // Verifies that after clicking cancel, the NavController navigates to the Client Menu destination
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_menu_client;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
        });
    }

    @Test
    public void testClearFieldsOnCancel() {
        // Simulates a click on the Cancel button
        onView(withId(R.id.btnCancel)).perform(click());

        // Verifies that after clicking cancel, all input fields are cleared
        fragmentScenario.onFragment(fragment -> {
            assertEquals("", fragment.getEtEmail().getText().toString());  // Verifies email field is cleared
            assertEquals("", fragment.getEtPhone().getText().toString());  // Verifies phone field is cleared
            assertEquals(0, fragment.getSpinnerLanguage().getSelectedItemPosition());  // Verifies language spinner is reset
            assertEquals(0, fragment.getSpinnerDistanceUnits().getSelectedItemPosition());  // Verifies distance units spinner is reset
            assertEquals(0, fragment.getSpinnerDistanceRadius().getSelectedItemPosition());  // Verifies distance radius spinner is reset
        });
    }

    @Test
    public void testLoadSettings() {
        // Calls the loadSettings() method on the fragment to load the settings
        fragmentScenario.onFragment(fragment -> {
            fragment.loadSettings();  // Loads the pre-set test data for the settings

            // Verifies that the settings fields are populated with the expected values
            assertEquals("test@example.com", fragment.getEtEmail().getText().toString());  // Verifies email field
            assertEquals("1234567890", fragment.getEtPhone().getText().toString());  // Verifies phone field
            assertEquals(1, fragment.getSpinnerLanguage().getSelectedItemPosition());  // Verifies language spinner
            assertEquals(0, fragment.getSpinnerDistanceUnits().getSelectedItemPosition());  // Verifies distance units spinner
            assertEquals(0, fragment.getSpinnerDistanceRadius().getSelectedItemPosition());  // Verifies distance radius spinner
        });
    }
}
