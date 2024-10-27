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

import com.example.poe2.MainActivity;
import com.example.poe2.R;
import com.example.poe2.ui.settings_dentist.SettingsDentistFragment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

// Adapted from: Android Testing Documentation
// Source URL: https://developer.android.com/training/testing
// Contributors: Android Developers
// Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
@RunWith(AndroidJUnit4.class) // Specifies that the tests should be run with the AndroidJUnit4 test runner
public class SettingsDentistTest {

    private FragmentScenario<SettingsDentistFragment> fragmentScenario;
    private TestNavHostController navController;

    @Before
    public void setup() {
        // Launch MainActivity and set up the navigation controller
        ActivityScenario<MainActivity> activityScenario = ActivityScenario.launch(MainActivity.class);

        activityScenario.onActivity(activity -> {
            navController = new TestNavHostController(activity);
            navController.setGraph(R.navigation.mobile_navigation); // Set the navigation graph
            navController.setCurrentDestination(R.id.nav_settings_dentist); // Set the current destination
        });

        // Launch the SettingsDentistFragment in a container
        fragmentScenario = FragmentScenario.launchInContainer(SettingsDentistFragment.class);

        fragmentScenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController); // Set the navigation controller for the fragment

            // Set initial values for the fragment's views
            fragment.getEtAddress().setText("123 Dentist Street");
            fragment.getEtPhoneD().setText("9876543210");
            fragment.getSpinnerLanguageD().setSelection(0);
        });
    }

    @Test
    public void testSaveButton() {
        // Perform click on the save button
        onView(withId(R.id.btnSaveD)).perform(click());

        // Check if the current destination is still the settings dentist fragment
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_settings_dentist;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
        });
    }

    @Test
    public void testCancelButton() {
        // Perform click on the cancel button
        onView(withId(R.id.btnCancelD)).perform(click());

        // Check if the current destination is the menu dentist fragment
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_menu_dentist;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
        });
    }

    @Test
    public void testClearFieldsOnCancel() {
        // Perform click on the cancel button
        onView(withId(R.id.btnCancelD)).perform(click());

        // Check if the fields are cleared
        fragmentScenario.onFragment(fragment -> {
            assertEquals("", fragment.getEtAddress().getText().toString());
            assertEquals("", fragment.getEtPhoneD().getText().toString());
            assertEquals(0, fragment.getSpinnerLanguageD().getSelectedItemPosition());
        });
    }

    @Test
    public void testLoadSettings() {
        // Load the settings and check if the fields are set correctly
        fragmentScenario.onFragment(fragment -> {
            fragment.loadLanguagePreference();

            assertEquals("123 Dentist Street", fragment.getEtAddress().getText().toString());
            assertEquals("9876543210", fragment.getEtPhoneD().getText().toString());
            assertEquals(0, fragment.getSpinnerLanguageD().getSelectedItemPosition());
        });
    }
}

