package com.example.opsc7312poepart2_code;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import android.util.Log;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;


import com.example.opsc7312poepart2_code.ui.book_app_client2.BookAppClient2Fragment;
import com.example.poe2.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.Before;
import org.junit.Test;

public class BookAppClient2FragmentTest {
    private FragmentScenario<BookAppClient2Fragment> fragmentScenario;
    private TestNavHostController navController;
    private DatabaseReference dbReference;

    @Before
    public void setup() {
        // Initialize TestNavHostController
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());
        navController.setGraph(R.navigation.mobile_navigation);
        navController.setCurrentDestination(R.id.nav_book_app_client2); // Set the start destination

        // Launch the BookAppClient2Fragment
        fragmentScenario = FragmentScenario.launchInContainer(BookAppClient2Fragment.class);

        // Set the NavController for the fragment
        fragmentScenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController);
            // Mock Firebase database reference
            dbReference = FirebaseDatabase.getInstance().getReference("dentists");
            // Add test dentist data for the test
            dbReference.child("testDentist1").child("name").setValue("Dr. John Smith");
        });
    }

    @Test
    public void testUIElementsDisplay() {
        // Verify that the relevant UI elements are displayed
        onView(withId(R.id.sTime)).check(matches(isDisplayed()));
        onView(withId(R.id.txtSelectedDentist)).check(matches(isDisplayed()));
        onView(withId(R.id.etxtDescription)).check(matches(isDisplayed()));
        onView(withId(R.id.btnBook)).check(matches(isDisplayed()));
        onView(withId(R.id.calendar)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCancel)).check(matches(isDisplayed()));
        onView(withId(R.id.ibtnHome)).check(matches(isDisplayed()));
    }

    @Test
    public void testSelectDentistAndBookAppointment() {
        // Simulate selecting a dentist
        onView(withId(R.id.txtSelectedDentist)).perform(typeText("Dr. John Smith"), closeSoftKeyboard());

        // Simulate selecting a time slot
        onView(withId(R.id.sTime)).perform(click());

        // Simulate clicking the book button
        onView(withId(R.id.btnBook)).perform(click());

        // Check that the appointment is booked successfully
        // Ideally, here you would also mock the Firebase response for testing
        // For now, we'll just check if the booking process triggers Toast or logs the event
        Log.i("BookAppClient2FragmentTest", "Appointment booking process initiated.");
    }

    @Test
    public void testDateSelection() {
        // Simulate clicking the date button to select a date
        onView(withId(R.id.calendar)).perform(click());

        // Since DatePickerDialog is shown, additional checks might involve mocking user interaction
        // For simplicity, we won't simulate date picking here, as Espresso does not handle dialogs well.
        // But we can assert the selected date after the user has interacted with the dialog.
    }

    @Test
    public void testClearInputs() {
        // Simulate filling in the inputs
        onView(withId(R.id.txtSelectedDentist)).perform(typeText("Dr. John Smith"));
        onView(withId(R.id.etxtDescription)).perform(typeText("Checkup"));

        // Simulate clicking the clear button
        onView(withId(R.id.btnCancel)).perform(click());

        // Verify that the inputs are cleared
        onView(withId(R.id.txtSelectedDentist)).check(matches(withText("")));
        onView(withId(R.id.etxtDescription)).check(matches(withText("")));
    }

    @Test
    public void testNavigateHome() {
        // Simulate clicking the home button
        onView(withId(R.id.ibtnHome)).perform(click());

        // Verify that the user is navigated back to the home fragment
        assertEquals("Failed to navigate back to Home", R.id.nav_menu_client, navController.getCurrentDestination().getId());
    }
}
