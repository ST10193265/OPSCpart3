package com.example.opsc7312poepart2_code;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.opsc7312poepart2_code.ui.register_client.RegisterClientFragment;
import com.example.poe2.MainActivity;
import com.example.poe2.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

@RunWith(AndroidJUnit4.class)
public class RegisterClientTest {

    private FragmentScenario<RegisterClientFragment> fragmentScenario;
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
            navController.setCurrentDestination(R.id.nav_register_client); // Set the start destination
        });

        // Launch the RegisterClientFragment
        fragmentScenario = FragmentScenario.launchInContainer(RegisterClientFragment.class);

        // Set the NavController for the fragment
        fragmentScenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController);
            // Mock Firebase database
            dbReference = FirebaseDatabase.getInstance().getReference("clients");
        });
    }

    @Test
    public void testSuccessfulRegistration() {
        // Simulate entering registration details
        onView(withId(R.id.etxtName)).perform(typeText("John"), closeSoftKeyboard());
        onView(withId(R.id.etxtSurname)).perform(typeText("Doe"), closeSoftKeyboard());
        onView(withId(R.id.etxtEmail)).perform(typeText("johndoe@example.com"), closeSoftKeyboard());
        onView(withId(R.id.etxtPhoneNumber)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.etxtUsername)).perform(typeText("johndoe"), closeSoftKeyboard());
        onView(withId(R.id.etxtPassword)).perform(replaceText("testPassword"), closeSoftKeyboard());



        // Click the register button
        onView(withId(R.id.btnRegister)).perform(click());

        // Add a delay to wait for the registration process to complete
        // Alternatively, you can use IdlingResource for better synchronization
        try {
            Thread.sleep(2000); // Wait for the registration process to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify that the user is navigated to the login screen after successful registration
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_login_client;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
            Log.i("RegisterClientTest", "Registration successful for user: johndoe");
        });
    }

    @Test
    public void testRegistrationWithEmptyFields() {
        // Click the register button without entering credentials
        onView(withId(R.id.btnRegister)).perform(click());

        // Verify that the user remains on the registration screen
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_register_client;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
            Log.w("RegisterClientTest", "Attempted registration with empty fields.");
        });
    }

//
}
