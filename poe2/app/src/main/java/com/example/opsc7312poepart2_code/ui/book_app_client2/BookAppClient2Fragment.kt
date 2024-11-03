package com.example.opsc7312poepart2_code.ui.book_app_client2

import android.content.Intent
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.opsc7312poepart2_code.ui.Appointments
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
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

    private lateinit var dentistDatabase: DatabaseReference
    private lateinit var appointmentssDatabase: DatabaseReference
    private lateinit var clientDatabase: DatabaseReference
    private lateinit var timeOffDatabase: DatabaseReference

    private lateinit var apiService: ApiService

    private var dentistId: String? = null
    private var userId: String? = null
    private var clientUsername: String? = null
    private var selectedDate: String? = null
    private var dentistUsername: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book_app_client2, container, false)

        txtSelectedDentist = view.findViewById(R.id.txtSelectedDentist)
        txtDate = view.findViewById(R.id.txtDate)
        calendarView = view.findViewById(R.id.calendar)
        spinnerTime = view.findViewById(R.id.sTime)
        etxtDescription = view.findViewById(R.id.etxtDescription)

        dentistDatabase = FirebaseDatabase.getInstance().getReference("dentists")
        appointmentssDatabase = FirebaseDatabase.getInstance().getReference("appointments")
        clientDatabase = FirebaseDatabase.getInstance().getReference("clients")
        timeOffDatabase = FirebaseDatabase.getInstance().getReference("timeOffBookings")

        apiService = ApiClient.createApiService(requireContext())

        val selectedDentist = arguments?.getString("selectedDentist")
        txtSelectedDentist.text = selectedDentist
        Log.d("BookAppClient2Fragment", "Selected Dentist: $selectedDentist")

        populateTimeSlots()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            txtDate.text = selectedDate
            Log.d("BookAppClient2Fragment", "Selected Date: $selectedDate")
        }

        selectedDentist?.let { getDentistIdByName(it) }
        getClientIdByUsername(LoginClientFragment.loggedInClientUsername ?: "")

        view.findViewById<View>(R.id.btnBook).setOnClickListener {
            if (selectedDate == null) {
                Toast.makeText(requireContext(), "Please select a date.", Toast.LENGTH_SHORT).show()
                Log.e("BookAppClient2Fragment", "Attempted to book appointment without selecting a date.")
                return@setOnClickListener
            }
            checkDentistTimeOffAndBook()
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            clearFields()
        }

        view.findViewById<View>(R.id.ibtnBack).setOnClickListener {
            findNavController().navigate(R.id.action_nav_book_app_client2_to_nav_book_app_client1)
        }

        return view
    }

    private fun populateTimeSlots() {
        val timeSlots = listOf(
            "Select a time",
            "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
            "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
        )

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
                        userId = clientSnapshot.key // Get the client ID from the snapshot
                        clientUsername = username // Store the retrieved username
                        Log.d("BookAppClient2Fragment", "Client ID found: $userId, Username: $clientUsername")
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

    private fun checkDentistTimeOffAndBook() {
        val selectedSlot = spinnerTime.selectedItem.toString()
        if (selectedSlot == "Select a time") {
            Toast.makeText(requireContext(), "Please select a time slot.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the dentist has time off on the selected date and time slot
        timeOffDatabase.child(dentistId ?: "").orderByChild("date").equalTo(selectedDate).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Dentist has time off on the selected date
                    Toast.makeText(requireContext(), "The dentist is unavailable on the selected date.", Toast.LENGTH_SHORT).show()
                } else {
                    // No time off; proceed to book the appointment
                    bookAppointment(selectedSlot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BookAppClient2Fragment", "Error checking time off: ${error.message}")
            }
        })
    }

    private fun bookAppointment(selectedSlot: String) {
        if (dentistId == null || userId == null || clientUsername == null) {
            Toast.makeText(requireContext(), "Please select a dentist and ensure you are logged in.", Toast.LENGTH_SHORT).show()
            Log.e("BookAppClient2Fragment", "Dentist ID or Client ID is null.")
            return
        }

        val description = etxtDescription.text.toString()

        val appointment = Appointments(
            date = selectedDate ?: "",
            dentist = dentistUsername ?: "",
            dentistId = dentistId!!,
            description = description,
            slot = selectedSlot,
            userId = userId!!,
            clientUsername = clientUsername ?: "", // Include client username
            status = "pending"
        )

        Log.d("BookAppClient2Fragment", "Booking appointment: $appointment with user ID: $userId")

        apiService.bookAppointment(appointment).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                    Log.d("BookAppClient2Fragment", "Appointment booked successfully for: ${appointment.clientUsername}")
                    clearFields()
                    findNavController().navigate(R.id.action_nav_book_app_client2_to_nav_book_app_client1)
                } else {
                    Toast.makeText(requireContext(), "Failed to book appointment. Please try again.", Toast.LENGTH_SHORT).show()
                    Log.e("BookAppClient2Fragment", "Failed to book appointment. Response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("BookAppClient2Fragment", "Error booking appointment: ${t.message}")
            }
        })
    }

    private fun clearFields() {
        txtSelectedDentist.text = ""
        txtDate.text = ""
        spinnerTime.setSelection(0)
        etxtDescription.text.clear()
        Log.d("BookAppClient2Fragment", "Fields cleared.")
    }
}
