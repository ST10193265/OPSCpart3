package com.example.opsc7312poepart2_code.ui.book_app_client2

import android.content.Context
import android.net.ConnectivityManager
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
import androidx.room.Room
import com.example.poe2.R
import com.example.opsc7312poepart2_code.ui.Appointments
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.AppDatabase
import com.example.opsc7312poepart2_code.ui.Appointments1
import com.example.opsc7312poepart2_code.ui.BookTimeOff
import com.example.opsc7312poepart2_code.ui.MIGRATION_1_2
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var appDatabase: AppDatabase

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

        appDatabase = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java, "app_database"
        )
            .addMigrations(MIGRATION_1_2) // Add your migration here
            .build()


        val selectedDentist = arguments?.getString("selectedDentist")
        txtSelectedDentist.text = selectedDentist
        Log.d("BookAppClient2Fragment", "Selected Dentist: $selectedDentist")

        populateTimeSlots()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            txtDate.text = selectedDate
            Log.d("BookAppClient2Fragment", "Selected Date: $selectedDate")
        }
        syncAppointmentsWithFirebase()
        selectedDentist?.let { getDentistIdByName(it) }
        getClientIdByUsername(LoginClientFragment.loggedInClientUsername ?: "")

        view.findViewById<View>(R.id.btnBook).setOnClickListener {
            if (selectedDate == null) {
                Toast.makeText(requireContext(), "Please select a date.", Toast.LENGTH_SHORT).show()
                Log.e("BookAppClient2Fragment", "Attempted to book appointment without selecting a date.")
                return@setOnClickListener
            }
            val selectedSlot = spinnerTime.selectedItem.toString()

            if (!isOnline()) {
                // Handle offline booking
                saveAppointmentOffline(selectedSlot)

            } else {
                checkDentistTimeOffAndBook(selectedSlot)
            }
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
        Log.d("BookAppClient2Fragment", "Fetching dentist ID for: $dentistName")
        dentistDatabase.orderByChild("name").equalTo(dentistName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (dentistSnapshot in snapshot.children) {
                        dentistId = dentistSnapshot.key
                        dentistUsername = dentistSnapshot.child("username").getValue(String::class.java)
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
                        userId = clientSnapshot.key
                        clientUsername = username
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

    private fun checkDentistTimeOffAndBook(selectedSlot: String) {
        if (dentistId == null || userId == null || clientUsername == null) {
            Toast.makeText(requireContext(), "Please select a dentist and ensure you are logged in.", Toast.LENGTH_SHORT).show()
            Log.e("BookAppClient2Fragment", "Dentist ID or Client ID is null.")
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

    private fun isDateInRange(dateToCheck: String, startDate: String, endDate: String): Boolean {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateCheck = dateFormatter.parse(dateToCheck)
        val dateStart = dateFormatter.parse(startDate)
        val dateEnd = dateFormatter.parse(endDate)

        return dateCheck != null && dateCheck in dateStart..dateEnd
    }

    private fun bookAppointment(selectedSlot: String) {
        if (!isOnline()) {
            // Handle offline booking
            saveAppointmentOffline(selectedSlot)
            return
        }

        if (dentistId == null || userId == null || clientUsername == null) {
            Toast.makeText(requireContext(), "Please select a dentist and ensure you are logged in.", Toast.LENGTH_SHORT).show()
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

        // Attempt to book via API
        apiService.bookAppointment(appointment).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                    clearFields()
                } else {
                    Log.e("BookAppClient2Fragment", "Failed to book appointment: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to book appointment, saved offline.", Toast.LENGTH_SHORT).show()
                    saveAppointmentOffline(selectedSlot) // Also save offline if booking fails
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("BookAppClient2Fragment", "API call failed: ${t.message}")
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                saveAppointmentOffline(selectedSlot) // Save offline on failure
            }
        })
    }

    private fun saveAppointmentOffline(selectedSlot: String) {
        val description = etxtDescription.text.toString()

        val appointment = Appointments1(
            date = selectedDate ?: "",
            dentist = dentistUsername ?: "",
            description = description,
            slot = selectedSlot,
            clientUsername = clientUsername ?: "",
            status = "pending"
        )

        // Save appointment to Room
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.appointmentDao().insert(appointment)

            // Switch to Main dispatcher to show Toast
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Appointment booked successfully OFFLINE!", Toast.LENGTH_SHORT).show()
                clearFields()
                findNavController().navigate(R.id.action_nav_book_app_client2_to_nav_book_app_client1)
            }
        }

    }


    private fun clearFields() {
        txtSelectedDentist.text = ""
        txtDate.text = ""
        spinnerTime.setSelection(0)
        etxtDescription.text.clear()
        selectedDate = null
        dentistId = null
        userId = null
        Log.d("BookAppClient2Fragment", "Fields cleared.")
    }
    private fun syncAppointmentsWithFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            val appointments = appDatabase.appointmentDao().getAllAppointments()
            for (appointment in appointments) {
                // Retrieve dentist ID and client ID
                val dentistName = appointment.dentist // Assuming appointment has a dentist property
               // getDentistIdByName(dentistName) // Fetch dentist ID asynchronously
               // getClientIdByUsername(appointment.clientUsername) // Fetch client ID asynchronously

                // Ensure dentistId and userId are populated before proceeding
                while (dentistId == null || userId == null) {
                    delay(100) // Wait until IDs are fetched (consider a better approach for production)
                }

                // Create a new appointment object with the necessary IDs
                val newAppointment = Appointments(
                    date = appointment.date,
                    dentist = dentistName,
                    dentistId = dentistId!!,
                    description = appointment.description,
                    slot = appointment.slot,
                    userId = userId!!,
                    clientUsername = appointment.clientUsername,
                    status = appointment.status
                )

                // Push to Firebase
                clientDatabase.child("appointments").push().setValue(newAppointment).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Switch to IO context for deletion to avoid main thread access
                        CoroutineScope(Dispatchers.IO).launch {
                            appDatabase.appointmentDao().delete(appointment)
                        }
                    } else {
                        Log.e("Sync", "Failed to sync appointment: ${task.exception?.message}")
                    }
                }
            }
        }
    }


    // Check network connectivity
    private fun isOnline(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
