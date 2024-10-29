package com.example.opsc7312poepart2_code.ui.book_app_client2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.opsc7312poepart2_code.ui.Appointments
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment
import com.google.firebase.database.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BookAppClient2Fragment : Fragment() {

    private lateinit var txtSelectedDentist: TextView
    private lateinit var txtDate: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var spinnerTime: Spinner
    private lateinit var etxtDescription: EditText

    private lateinit var dentistDatabase: DatabaseReference // Reference for dentists
    private lateinit var appointmentssDatabase: DatabaseReference
    private lateinit var clientDatabase: DatabaseReference

    private lateinit var apiService: ApiService // Declare ApiService instance

    private var dentistId: String? = null
    private var userId: String? = null
    private var selectedDate: String? = null
    private var dentistUsername: String? = null // Variable to hold dentist's username

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_book_app_client2, container, false)

        // Initialize views
        txtSelectedDentist = view.findViewById(R.id.txtSelectedDentist)
        txtDate = view.findViewById(R.id.txtDate)
        calendarView = view.findViewById(R.id.calendar)
        spinnerTime = view.findViewById(R.id.sTime)
        etxtDescription = view.findViewById(R.id.etxtDescription)

        // Initialize Firebase Database reference for dentists
        dentistDatabase = FirebaseDatabase.getInstance().getReference("dentists") // Dentists reference
        appointmentssDatabase = FirebaseDatabase.getInstance().getReference("appointments")
        clientDatabase = FirebaseDatabase.getInstance().getReference("clients")

        apiService = ApiClient.createApiService(requireContext()) // Initialize ApiService

        // Get the selected dentist's name from arguments and set it to the TextView
        val selectedDentist = arguments?.getString("selectedDentist")
        txtSelectedDentist.text = selectedDentist
        Log.d("BookAppClient2Fragment", "Selected Dentist: $selectedDentist")

        // Populate time slots
        populateTimeSlots()

        // Set up the CalendarView listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            txtDate.text = selectedDate // Populate txtDate with the selected date
            Log.d("BookAppClient2Fragment", "Selected Date: $selectedDate")
        }

        // Load selected dentist ID and username
        if (selectedDentist != null) {
            getDentistIdByName(selectedDentist)
        }

        // Load client ID
        getClientIdByUsername(LoginClientFragment.loggedInClientUsername ?: "")

        // Set up the Book button listener to book the appointment
        view.findViewById<View>(R.id.btnBook).setOnClickListener {
            if (selectedDate == null) {
                Toast.makeText(requireContext(), "Please select a date.", Toast.LENGTH_SHORT).show()
                Log.e("BookAppClient2Fragment", "Attempted to book appointment without selecting a date.")
                return@setOnClickListener
            }
            bookAppointment()
        }

        // Set up the Cancel button listener to clear fields
        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            clearFields() // Call clearFields when cancel is clicked
        }

        view.findViewById<View>(R.id.ibtnBack).setOnClickListener{
            findNavController().navigate(R.id.action_nav_book_app_client2_to_nav_book_app_client1)
        }

        return view
    }

    private fun populateTimeSlots() {
        // Define the hourly intervals as strings
        val timeSlots = listOf(
            "Select a time",
            "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
            "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
        )

        // Set the adapter for the spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeSlots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTime.adapter = adapter
    }

    private fun getDentistIdByName(dentistName: String) {
        Log.d("BookAppClient2Fragment", "Fetching dentist ID for: $dentistName")
        dentistDatabase.orderByChild("name").equalTo(dentistName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dentistSnapshot in snapshot.children) {
                        dentistId = dentistSnapshot.key // Get the dentist ID
                        dentistUsername = dentistSnapshot.child("username").getValue(String::class.java) // Retrieve dentist's username
                        Log.d("BookAppClient2Fragment", "Dentist ID found: $dentistId, Username: $dentistUsername")
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

    private fun getClientIdByUsername(username: String) {
        Log.d("BookAppClient2Fragment", "Fetching client ID for: $username")
        clientDatabase.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (clientSnapshot in snapshot.children) {
                        userId = LoginClientFragment.loggedInClientUserId ?: "" // Get the client ID
                        Log.d("BookAppClient2Fragment", "Client ID found: $userId")
                    }
                } else {
                    Log.e("BookAppClient2Fragment", "Client not found.")
                    Toast.makeText(requireContext(), "Client not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BookAppClient2Fragment", "Error fetching client ID: ${error.message}")
            }
        })
    }

    private fun bookAppointment() {
        // Check if dentistId and clientId are available
        if (dentistId == null || userId == null) {
            Toast.makeText(requireContext(), "Please select a dentist and ensure you are logged in.", Toast.LENGTH_SHORT).show()
            Log.e("BookAppClient2Fragment", "Dentist ID or Client ID is null.")
            return
        }

        // Get selected date, time slot, and description
        val selectedDate = txtDate.text.toString() // e.g., "2024-10-30"
        val selectedSlot = spinnerTime.selectedItem.toString() // e.g., "10:00 AM"
        val description = etxtDescription.text.toString() // e.g., "Checkup visit"

        // Create appointment instance
        val appointment = Appointments(
            date = selectedDate,
            dentist = dentistUsername ?: "", // Use dentistUsername here
            dentistId = dentistId!!,  // Ensure dentistId is not null
            description = description,
            slot = selectedSlot,
            userId = userId!!,     // Ensure clientId is not null
            status = "pending" // Setting initial status to pending
        )

        Log.d("BookAppClient2Fragment", "Booking appointment: $appointment with user ID: $userId")

        // Make the API call to book the appointment, including user ID in the request
        apiService.bookAppointment(appointment).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                    Log.d("BookAppClient2Fragment", "Appointment booked successfully: ${response.body()}")
                    clearFields()
                } else {
                    Log.e("BookAppClient2Fragment", "Failed to book appointment: ${response.code()} - ${response.message()}")
                    response.errorBody()?.let { errorBody ->
                        Log.e("BookAppClient2Fragment", "Response body: ${errorBody.string()}")
                    }
                    Toast.makeText(requireContext(), "Failed to book appointment: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("BookAppClient2Fragment", "Error booking appointment: ${t.message}")
                Toast.makeText(requireContext(), "Error booking appointment: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun clearFields() {
        // Clear the input fields
        txtDate.text = ""
        txtSelectedDentist.text = ""
        etxtDescription.text.clear()
        calendarView.setDate(System.currentTimeMillis(), true, true)
        spinnerTime.setSelection(0) // Reset the spinner to the first item
        Log.d("BookAppClient2Fragment", "Fields cleared after booking appointment.")
    }
}
