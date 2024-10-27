package com.example.opsc7312poepart2_code;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

import android.util.Log;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment;
import com.example.poe2.MainActivity;
import com.example.poe2.R;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

@RunWith(AndroidJUnit4.class)
public class LoginClientTest {

    private FragmentScenario<LoginClientFragment> fragmentScenario;
    private TestNavHostController navController;
    private DatabaseReference dbReference;

    @Before
    public void setup() {
        // Launch MainActivity
        ActivityScenario<MainActivity> activityScenario = ActivityScenario.launch(MainActivity.class);

        // Initialize TestNavHostController
        activityScenario.onActivity(activity -> {
            navController = new TestNavHostController(activity);
            navController.setGraph(R.navigation.mobile_navigation);
            navController.setCurrentDestination(R.id.nav_login_client); // Set the start destination
        });

        // Launch the LoginClientFragment
        fragmentScenario = FragmentScenario.launchInContainer(LoginClientFragment.class);

        // Set the NavController for the fragment
        fragmentScenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController);
            // Mock Firebase database and create test user
            dbReference = FirebaseDatabase.getInstance().getReference("clients");
            // Add the test user data
            dbReference.child("testUser").child("password").setValue("testPassword");
        });
    }

    @Test
    public void testSuccessfulLogin() {
        // Simulate entering username and password
        onView(withId(R.id.etxtUsername)).perform(typeText("johndoe"), closeSoftKeyboard());
        onView(withId(R.id.etxtPassword)).perform(replaceText("testPassword"), closeSoftKeyboard());

        // Click the login button
        onView(withId(R.id.btnLogin)).perform(click());

        // Wait for a short period to allow for navigation to complete
        try {
            Thread.sleep(2000); // Wait for 2 seconds (adjust as needed)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify that the user is navigated to the menu after successful login
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_menu_client;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
            Log.i("LoginClientTest", "Login successful for user: testUser");
        });
    }

    @Test
    public void testLoginWithEmptyFields() {
        // Click the login button without entering credentials
        onView(withId(R.id.btnLogin)).perform(click());

        // Verify that the user remains on the login screen
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_login_client;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
            Log.w("LoginClientTest", "Attempted login with empty fields.");
        });
    }
}
