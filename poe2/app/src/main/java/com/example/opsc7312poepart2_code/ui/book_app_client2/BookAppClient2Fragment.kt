package com.example.opsc7312poepart2_code.ui.book_app_client2

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.example.poe2.R
import com.google.firebase.database.*
import java.util.*

class BookAppClient2Fragment : Fragment() {

    // UI elements
    private lateinit var spinnerSlots: Spinner
    private lateinit var txtSelectedDentist: TextView
    private lateinit var editTextDescription: EditText
    private lateinit var btnBook: Button
    private lateinit var btnDate: Button
    private lateinit var btnCancel: Button
    private lateinit var btnHome: ImageButton

    // Firebase Database references
    private lateinit var database: DatabaseReference
    private lateinit var dentistDatabase: DatabaseReference // Reference for dentists

    // Variables to hold selected date and dentist ID
    private var selectedDate: String? = null // Variable to store the selected date
    private var dentistId: String? = null // Variable to hold the dentist ID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_book_app_client2, container, false)

        // Initialize UI elements
        spinnerSlots = view.findViewById(R.id.sTime)
        txtSelectedDentist = view.findViewById(R.id.txtSelectedDentist)
        editTextDescription = view.findViewById(R.id.etxtDescription)
        btnBook = view.findViewById(R.id.btnBook)
        btnDate = view.findViewById(R.id.btnDate)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnHome = view.findViewById(R.id.ibtnHome)

        // Initialize Firebase Database references
        database = FirebaseDatabase.getInstance().getReference("appointments") // Appointments reference
        dentistDatabase = FirebaseDatabase.getInstance().getReference("dentists") // Dentists reference

        // Get the selected dentist's name from arguments and set it to the TextView
        val selectedDentist = arguments?.getString("selectedDentist")
        txtSelectedDentist.text = selectedDentist
        Log.d("BookAppClient2Fragment", "Selected Dentist: $selectedDentist")

        // Populate the spinner with hourly slots
        val slots = generateTimeSlots(8, 16) // From 8 AM to 4 PM
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, slots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSlots.adapter = adapter

        // Set click listener for the date button
        btnDate.setOnClickListener {
            // Show date picker dialog for selecting a date
            showDatePicker()
        }

        // Set click listener for the cancel button
        btnCancel.setOnClickListener {
            // Clear all input fields
            clearInputs()
        }

        // Set click listener for the home button
        btnHome.setOnClickListener {
            // Navigate back to home
            findNavController().navigate(R.id.action_nav_book_app_client2_to_nav_menu_client)
        }

        // Set click listener for the book button
        btnBook.setOnClickListener {
            val selectedSlot = spinnerSlots.selectedItem.toString() // Get the selected time slot
            val description = editTextDescription.text.toString() // Get the description

            // Check if a date has been selected before booking
            if (selectedDate != null) {
                Log.d("BookAppClient2Fragment", "Fetching dentist ID for: $selectedDentist")
                getDentistIdByName(selectedDentist ?: "")
            } else {
                Toast.makeText(requireContext(), "Please select a date.", Toast.LENGTH_SHORT).show()
                Log.w("BookAppClient2Fragment", "Date not selected before booking attempt.")
            }
        }

        return view
    }

    // Function to generate time slots from startHour to endHour
    private fun generateTimeSlots(startHour: Int, endHour: Int): List<String> {
        val slots = mutableListOf<String>()
        for (hour in startHour until endHour + 1) { // Include endHour
            for (minute in listOf(0, 30)) { // Every 30 minutes
                val time = String.format(
                    "%02d:%02d %s",
                    hour % 12, minute, if (hour < 12) "AM" else "PM"
                )
                slots.add(time)
                Log.d("BookAppClient2Fragment", "Generated time slot: $time") // Log each generated slot
            }
        }
        return slots
    }

    // Method to clear and reset the spinner and inputs
    private fun clearInputs() {
        spinnerSlots.setSelection(0) // Reset spinner to first item
        txtSelectedDentist.text = "" // Clear dentist text
        editTextDescription.text.clear() // Clear description text
        selectedDate = null // Reset selected date
        Log.d("BookAppClient2Fragment", "Inputs cleared.")
    }

    // Function to show a date picker dialog
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear" // Format the date
                Log.d("BookAppClient2Fragment", "Selected Date: $selectedDate") // Log the selected date
            },
            year, month, day
        )

        datePickerDialog.show()
        Log.d("BookAppClient2Fragment", "DatePicker dialog shown.")
    }

    // Method to get the dentist ID based on the selected dentist's name
    private fun getDentistIdByName(dentistName: String) {
        Log.d("BookAppClient2Fragment", "Fetching dentist ID for: $dentistName")
        dentistDatabase.orderByChild("name").equalTo(dentistName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dentistSnapshot in snapshot.children) {
                        dentistId = dentistSnapshot.key // Get the dentist ID
                        Log.d("BookAppClient2Fragment", "Dentist ID found: $dentistId")
                        val selectedSlot = spinnerSlots.selectedItem.toString()
                        // Proceed to book appointment with the fetched dentist ID
                        bookAppointment(dentistName, selectedSlot, selectedDate!!, editTextDescription.text.toString(), loggedInClientUserId ?: "", dentistId ?: "")
                    }
                } else {
                    Log.e("BookAppClient2Fragment", "Dentist not found.")
                    Toast.makeText(requireContext(), "Dentist not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BookAppClient2Fragment", "Error fetching dentist ID: ${error.message}")
            }
        })
    }

    // Function to book the appointment and save to Firebase database
    private fun bookAppointment(dentist: String, slot: String, date: String, description: String, clientId: String, dentistId: String?) {
        // Generate a unique ID for each booking
        val appointmentId = database.push().key

        if (appointmentId != null) {
            // Prepare booking details
            val bookingDetails = mapOf(
                "appointmentId" to appointmentId, // Saving the unique appointment ID
                "dentist" to dentist,              // Saving the selected dentist's name
                "slot" to slot,                    // Saving the selected time slot
                "date" to date,                    // Saving the selected date
                "description" to description,      // Saving the description
                "userId" to clientId,              // Saving the user ID
                "dentistId" to dentistId           // Saving the dentist ID
            )

            // Log the booking details before saving
            Log.d("BookAppClient2Fragment", "Booking Details: $bookingDetails")

            // Save booking details to Firebase under "appointments"
            database.child(appointmentId).setValue(bookingDetails)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                    clearInputs()
                }
                .addOnFailureListener { error ->
                    Log.e("BookAppClient2Fragment", "Failed to book appointment: ${error.message}")
                    Toast.makeText(requireContext(), "Failed to book appointment. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("BookAppClient2Fragment", "Failed to generate appointment ID")
        }
    }
}
