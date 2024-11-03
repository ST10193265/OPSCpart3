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

import com.example.opsc7312poepart2_code.ui.book_app_client1.BookAppClient1Fragment;
import com.example.poe2.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.Before;
import org.junit.Test;

public class BookAppointmentClient1Test {
    private FragmentScenario<BookAppClient1Fragment> fragmentScenario;
    private TestNavHostController navController;
    private DatabaseReference dbReference;

    @Before
    public void setup() {
        // Initialize TestNavHostController
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());
        navController.setGraph(R.navigation.mobile_navigation);
        navController.setCurrentDestination(R.id.nav_book_app_client1); // Set the start destination

        // Launch the BookAppClient1Fragment
        fragmentScenario = FragmentScenario.launchInContainer(BookAppClient1Fragment.class);

        // Set the NavController for the fragment
        fragmentScenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController);
            // Mock Firebase database reference
            dbReference = FirebaseDatabase.getInstance().getReference("dentists");
            // Add test dentist data
            dbReference.child("testDentist1").child("name").setValue("testDentist");
        });
    }



    @Test
    public void testSelectDentistAndNavigate() {
        // Simulate clicking on a dentist in the ListView
        onView(withText("testDentist")).perform(click());

        // Verify that the user is navigated to the BookAppClient2Fragment
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_book_app_client2; // Replace with actual ID
            assertEquals("Failed to navigate to BookAppClient2Fragment", expectedDestinationId, navController.getCurrentDestination().getId());
            Log.i("BookAppClient1FragmentTest", "Navigation to BookAppClient2Fragment successful.");
        });
    }
}
