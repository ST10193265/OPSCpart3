package com.example.poe2.ui.book_appointment_dentist

import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.loggedInDentistUserId
import java.text.SimpleDateFormat
import java.util.Locale
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.ParseException
import java.util.*

class BookAppointmentDentistFragment : Fragment() {

    private lateinit var calendarView1: CalendarView // Calendar view for selecting appointment dates
    private lateinit var appointmentListView1: ListView // List view for displaying appointments
    private lateinit var database: DatabaseReference // Reference to the Firebase database

    // Mutable map to store appointments for the dentist, organized by date
    private val appointments = mutableMapOf<String, List<String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_book_appointment_dentist, container, false)

        // Initialize the ImageButton for navigation
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)
        // Set OnClickListener to navigate back to the dentist menu when the button is clicked
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_book_appointment_dentist_to_nav_menu_dentist)
        }

        // Initialize Firebase Database reference to access appointments data
        database = FirebaseDatabase.getInstance().getReference("appointments")

        // Initialize the CalendarView and ListView for displaying appointments
        calendarView1 = view.findViewById(R.id.calendarView1)
        appointmentListView1 = view.findViewById(R.id.appointmentListView1)

        // Load appointments specific to the logged-in dentist
        val dentistId = loggedInDentistUserId
        if (dentistId != null) {
            loadDentistAppointments(dentistId) // Load appointments if dentist is logged in
        } else {
            // Show a toast message if the dentist is not logged in
            Toast.makeText(requireContext(), "Dentist not logged in.", Toast.LENGTH_SHORT).show()
        }

        return view // Return the inflated view
    }

    // Function to load appointments for the logged-in dentist
    // Adapted from: Firebase Realtime Database Documentation
    // Source URL: https://firebase.google.com/docs/database/android/start
    // Contributors: Firebase Developers
    // Contributor Profile: https://firebase.google.com/profile/u/0/FirebaseDevelopers
    private fun loadDentistAppointments(dentistId: String) {
        // Access all appointments from the database
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                appointments.clear() // Clear existing appointments before loading new data

                // Loop through each appointment in the database
                for (appointmentSnapshot in snapshot.children) {
                    // Extract the dentistId associated with this appointment
                    val appointmentDentistId = appointmentSnapshot.child("dentistId").getValue(String::class.java)

                    // Check if the appointment belongs to the logged-in dentist
                    if (appointmentDentistId == dentistId) {
                        // Extract appointment details
                        val appointmentDate = appointmentSnapshot.child("date").getValue(String::class.java) ?: continue
                        val appointmentTime = appointmentSnapshot.child("slot").getValue(String::class.java) ?: continue
                        val appointmentDesc = appointmentSnapshot.child("description").getValue(String::class.java) ?: ""

                        // Combine time and description for display
                        val appointmentDetails = "$appointmentTime - $appointmentDesc"

                        // Group appointments by date
                        if (appointments.containsKey(appointmentDate)) {
                            // Add appointment details to existing date's list
                            appointments[appointmentDate] = appointments[appointmentDate]!! + appointmentDetails
                        } else {
                            // Create a new list for appointments on a new date
                            appointments[appointmentDate] = listOf(appointmentDetails)
                        }
                    }
                }

                // Highlight appointment days on the calendar
                highlightAppointmentDays()

                // Set up click listener for calendar day selection
                calendarView1.setOnDayClickListener(object : OnDayClickListener {
                    override fun onDayClick(eventDay: EventDay) {
                        // Format the selected date from the calendar
                        val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
                        val selectedDate = dateFormat.format(eventDay.calendar.time)

                        // Retrieve the appointment list for the selected date
                        val appointmentList = appointments[selectedDate]

                        // Update the ListView with the appointment details or show a message if none exist
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            appointmentList ?: listOf("No appointments") // Show default message if no appointments found
                        )
                        appointmentListView1.adapter = adapter
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                // Log the error and show a toast message if loading appointments fails
                Log.e("BookAppointmentDentistFragment", "Failed to load appointments: ${error.message}")
                Toast.makeText(requireContext(), "Failed to load appointments.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Function to highlight days on the calendar that have appointments
    // Adapted from community contributions and best practices in Android development
    // Contributors: Stack Overflow community, Android Developer Documentation, and various open-source projects on GitHub
    // URL: https://stackoverflow.com, https://developer.android.com, https://github.com
    private fun highlightAppointmentDays() {
        // Retrieve the dates with appointments and parse them to Calendar instances
        val datesWithAppointments = appointments.keys.mapNotNull { dateString ->
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            try {
                val date = dateFormat.parse(dateString)
                Calendar.getInstance().apply { time = date }
            } catch (e: ParseException) {
                // Log error for invalid date format
                Log.e("BookAppointmentDentistFragment", "Invalid date format: $dateString")
                null
            }
        }

        // Create a list of EventDays for highlighting on the calendar
        val events = datesWithAppointments.map { calendar ->
            EventDay(calendar, R.drawable.redcircle, Color.RED) // Customize the event appearance
        }

        // Set the highlighted events on the calendar view
        calendarView1.setEvents(events)
    }
}

