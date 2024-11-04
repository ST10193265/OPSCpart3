package com.example.opsc7312poepart2_code.ui.book_app_client2

import android.os.Bundle
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
import com.example.opsc7312poepart2_code.ui.BookTimeOff
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment
import com.google.firebase.database.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class BookAppClient2Fragment : Fragment() {

    private lateinit var txtSelectedDentist: TextView
    private lateinit var txtDate: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var spinnerTime: Spinner
    private lateinit var etxtDescription: EditText

    private lateinit var dentistDatabase: DatabaseReference
    private lateinit var clientDatabase: DatabaseReference
    private lateinit var apiService: ApiService

    private var dentistId: String? = null
    private var userId: String? = null
    private var clientUsername: String? = null
    private var selectedDate: String? = null
    private var dentistUsername: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_book_app_client2, container, false)

        txtSelectedDentist = view.findViewById(R.id.txtSelectedDentist)
        txtDate = view.findViewById(R.id.txtDate)
        calendarView = view.findViewById(R.id.calendar)
        spinnerTime = view.findViewById(R.id.sTime)
        etxtDescription = view.findViewById(R.id.etxtDescription)

        dentistDatabase = FirebaseDatabase.getInstance().getReference("dentists")
        clientDatabase = FirebaseDatabase.getInstance().getReference("clients")
        apiService = ApiClient.createApiService(requireContext())

        val selectedDentist = arguments?.getString("selectedDentist")
        txtSelectedDentist.text = selectedDentist
        // Log selected dentist's name for debugging purposes
        // Log.d("BookAppClient2Fragment", "Selected Dentist: $selectedDentist")

        populateTimeSlots()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            txtDate.text = selectedDate
            // Log selected date for debugging purposes
            // Log.d("BookAppClient2Fragment", "Selected Date: $selectedDate")
        }

        selectedDentist?.let { getDentistIdByName(it) }
        getClientIdByUsername(LoginClientFragment.loggedInClientUsername ?: "")

        view.findViewById<View>(R.id.btnBook).setOnClickListener {
            if (selectedDate == null) {
                Toast.makeText(requireContext(), "Please select a date.", Toast.LENGTH_SHORT).show()
                // Log error if attempting to book without a selected date
                // Log.e("BookAppClient2Fragment", "Attempted to book appointment without selecting a date.")
                return@setOnClickListener
            }
            val selectedSlot = spinnerTime.selectedItem.toString()
            checkDentistTimeOffAndBook(selectedSlot)
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener { clearFields() }

        view.findViewById<View>(R.id.ibtnBack).setOnClickListener {
            findNavController().navigate(R.id.action_nav_book_app_client2_to_nav_book_app_client1)
        }

        return view
    }

    private fun populateTimeSlots() {
        val timeSlots = listOf("Select a time", "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM", "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeSlots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTime.adapter = adapter
    }

    private fun getDentistIdByName(dentistName: String) {
        // Log fetching attempt for dentist ID by name
        // Log.d("BookAppClient2Fragment", "Fetching dentist ID for: $dentistName")
        dentistDatabase.orderByChild("name").equalTo(dentistName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dentistSnapshot in snapshot.children) {
                        dentistId = dentistSnapshot.key
                        dentistUsername = dentistSnapshot.child("username").getValue(String::class.java)
                        // Log found dentist ID and username
                        // Log.d("BookAppClient2Fragment", "Dentist ID found: $dentistId, Username: $dentistUsername")
                    }
                } else {
                    // Log error if dentist is not found
                    // Log.e("BookAppClient2Fragment", "Dentist not found.")
                    Toast.makeText(requireContext(), "Dentist not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Log error in fetching dentist ID
                // Log.e("BookAppClient2Fragment", "Error fetching dentist ID: ${error.message}")
            }
        })
    }

    private fun getClientIdByUsername(username: String) {
        // Log fetching attempt for client ID by username
        // Log.d("BookAppClient2Fragment", "Fetching client ID for: $username")
        clientDatabase.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (clientSnapshot in snapshot.children) {
                        userId = clientSnapshot.key
                        clientUsername = username
                        // Log found client ID and username
                        // Log.d("BookAppClient2Fragment", "Client ID found: $userId, Username: $clientUsername")
                    }
                } else {
                    // Log error if client is not found
                    // Log.e("BookAppClient2Fragment", "Client not found.")
                    Toast.makeText(requireContext(), "Client not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Log error in fetching client ID
                // Log.e("BookAppClient2Fragment", "Error fetching client ID: ${error.message}")
            }
        })
    }

    private fun checkDentistTimeOffAndBook(selectedSlot: String) {
        if (dentistId == null || userId == null || clientUsername == null) {
            Toast.makeText(requireContext(), "Please select a dentist and ensure you are logged in.", Toast.LENGTH_SHORT).show()
            // Log error if dentist or client ID is null
            // Log.e("BookAppClient2Fragment", "Dentist ID or Client ID is null.")
            return
        }

        FirebaseDatabase.getInstance().getReference("booktimeoff")
            .orderByChild("dentistId").equalTo(dentistId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var isAvailable = true

                    for (timeOffSnapshot in snapshot.children) {
                        val timeOff = timeOffSnapshot.getValue(BookTimeOff::class.java)
                        if (timeOff != null && isDateInRange(selectedDate ?: "", timeOff.startDate, timeOff.endDate)) {
                            isAvailable = false
                            break
                        }
                    }

                    if (isAvailable) {
                        bookAppointment(selectedSlot)
                    } else {
                        Toast.makeText(requireContext(), "The dentist is not available on the selected date.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to check availability.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun bookAppointment(selectedSlot: String) {
        if (dentistId == null || userId == null || clientUsername == null) {
            Toast.makeText(requireContext(), "Please select a dentist and ensure you are logged in.", Toast.LENGTH_SHORT).show()
            // Log error if dentist or client ID is null when booking
            // Log.e("BookAppClient2Fragment", "Dentist ID or Client ID is null.")
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
            clientUsername = clientUsername ?: "",
            status = "pending"
        )

        // Log the appointment details being booked
        // Log.d("BookAppClient2Fragment", "Booking appointment for user: $userId")

        FirebaseDatabase.getInstance().getReference("appointments").push()
            .setValue(appointment)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(requireContext(), "Appointment booked successfully.", Toast.LENGTH_SHORT).show()
                    // Log success message for booking
                    // Log.d("BookAppClient2Fragment", "Appointment booked successfully.")
                } else {
                    Toast.makeText(requireContext(), "Failed to book appointment.", Toast.LENGTH_SHORT).show()
                    // Log failure message for booking
                    // Log.e("BookAppClient2Fragment", "Failed to book appointment.")
                }
            }
    }

    private fun isDateInRange(selectedDate: String, startDate: String, endDate: String): Boolean {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val selected = dateFormat.parse(selectedDate)
        val start = dateFormat.parse(startDate)
        val end = dateFormat.parse(endDate)

        return selected != null && start != null && end != null && selected in start..end
    }

    private fun clearFields() {
        spinnerTime.setSelection(0)
        txtDate.text = ""
        etxtDescription.setText("")
        Toast.makeText(requireContext(), "Fields cleared.", Toast.LENGTH_SHORT).show()
    }
}
