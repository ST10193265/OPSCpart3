package com.example.poe2.ui.calendar_client

import android.graphics.Color
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.applandeo.materialcalendarview.CalendarView
import com.example.poe2.R
import java.text.SimpleDateFormat
import java.util.Locale
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import java.text.ParseException

class CalendarClientFragment : Fragment() {

    private lateinit var calendarView: CalendarView // CalendarView for displaying dates
    private lateinit var appointmentListView: ListView // ListView for showing user's appointments
    private lateinit var database: DatabaseReference // Reference to Firebase Database

    // Map to store user appointments, keyed by date
    private var userAppointments = mutableMapOf<String, List<String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_calendar_client, container, false)

        // Initialize the Home button and set up its click listener
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)
        ibtnHome.setOnClickListener {
            // Navigate back to the Client Menu using NavController
            findNavController().navigate(R.id.action_nav_calendar_client_to_nav_menu_client)
        }

        // Initialize the CalendarView and ListView components
        calendarView = view.findViewById(R.id.calendarView)
        appointmentListView = view.findViewById(R.id.appointmentListView)

        // Initialize the Firebase Database reference to access appointments data
        database = FirebaseDatabase.getInstance().getReference("appointments")

        // Get the client ID for Firebase (ensure this variable is set during user login)
        val clientId = loggedInClientUserId

        // Load appointments specific to the logged-in user, if client ID is valid
        if (clientId != null) {
            loadUserAppointments(clientId)
        } else {
            // Show a toast message if the user is not logged in
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
        }

        return view // Return the inflated view
    }

    // Function to load appointments for the logged-in user from the database
    private fun loadUserAppointments(clientId: String) {
        // Access all appointments from the database
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userAppointments.clear()  // Clear existing appointments before loading new data

                // Loop through each appointment in the database snapshot
                for (appointmentSnapshot in snapshot.children) {
                    // Extract the userId associated with this appointment
                    val appointmentUserId = appointmentSnapshot.child("userId").getValue(String::class.java)

                    // Check if the appointment belongs to the logged-in user
                    if (appointmentUserId == clientId) {
                        // Extract appointment details
                        val appointmentDate = appointmentSnapshot.child("date").getValue(String::class.java) ?: continue
                        val appointmentTime = appointmentSnapshot.child("slot").getValue(String::class.java) ?: continue
                        val appointmentDesc = appointmentSnapshot.child("description").getValue(String::class.java) ?: ""

                        // Combine time and description for display
                        val appointmentDetails = "$appointmentTime - $appointmentDesc"

                        // Group appointments by date
                        userAppointments[appointmentDate] = userAppointments.getOrDefault(appointmentDate, listOf()) + appointmentDetails
                    }
                }

                // Highlight dates with appointments on the calendar
                highlightAppointmentDays()

                // Set up the click listener for calendar days
                calendarView.setOnDayClickListener(object : OnDayClickListener {
                    override fun onDayClick(eventDay: EventDay) {
                        val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())

                        // Get the selected date from the calendar
                        val selectedDate = dateFormat.format(eventDay.calendar.time)
                        // Log.d("CalendarClientFragment", "Selected date: $selectedDate") // Commented out

                        // Check if the selected date exists in userAppointments
                        val appointmentList = userAppointments[selectedDate]

                        // Update the appointment ListView based on selected date
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            appointmentList ?: listOf("No appointments") // Show default message if no appointments
                        )
                        appointmentListView.adapter = adapter
                    }
                })
            }

            // Handle errors when loading appointments from the database
            override fun onCancelled(error: DatabaseError) {
                // Log.e("CalendarClientFragment", "Failed to load appointments: ${error.message}") // Commented out
                Toast.makeText(requireContext(), "Failed to load appointments.", Toast.LENGTH_SHORT).show()
            }
        })
    }
    // The logic to load appointments is adapted from Firebase documentation
    // https://firebase.google.com/docs/database/android/read-and-write

    // Function to highlight days on the calendar that have appointments
    private fun highlightAppointmentDays() {
        // Parse the dates with appointments into Calendar instances
        val datesWithAppointments = userAppointments.keys.mapNotNull { dateString ->
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            try {
                val date = dateFormat.parse(dateString)
                Calendar.getInstance().apply { time = date } // Create Calendar instance
            } catch (e: ParseException) {
                // Log.e("CalendarClientFragment", "Invalid date format: $dateString") // Commented out
                null // Return null for invalid date formats
            }
        }

        // Create EventDay objects for the calendar
        val events = datesWithAppointments.map { calendar ->
            EventDay(calendar, R.drawable.redcircle, Color.RED) // Customize event appearance as needed
        }

        // Set the events on the CalendarView
        calendarView.setEvents(events)
    }
    // The logic for handling dates and events is adapted from various online resources
// Java SimpleDateFormat documentation: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html


}
