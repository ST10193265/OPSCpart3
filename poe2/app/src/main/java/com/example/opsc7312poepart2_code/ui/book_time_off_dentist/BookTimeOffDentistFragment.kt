package com.example.opsc7312poepart2_code.ui.book_time_off_dentist

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class BookTimeOffDentistFragment : Fragment() {

    // UI elements
    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var editTextReason: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button
    private lateinit var btnHome: ImageButton

    // Firebase Database reference
    private lateinit var database: DatabaseReference

    // Store dentist ID
    private var currentDentistId: String = ""

    // Variables to hold selected dates
    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.book_time_off, container, false)

        // Initialize UI elements
        btnStartDate = view.findViewById(R.id.btnStartDate)
        btnEndDate = view.findViewById(R.id.btnEndDate)
        editTextReason = view.findViewById(R.id.etxtReason)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnHome = view.findViewById(R.id.ibtnHome)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("timeoff")

        // Set click listeners
        btnStartDate.setOnClickListener {
            showDatePicker(true)
        }

        btnEndDate.setOnClickListener {
            showDatePicker(false)
        }

        btnSubmit.setOnClickListener {
            submitTimeOff()
        }

        btnCancel.setOnClickListener {
            clearInputs()
        }

        btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_book_time_off_to_nav_menu_dentist)
        }

        return view
    }



    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                if (isStartDate) {
                    selectedStartDate = selectedDate
                    btnStartDate.text = selectedDate
                } else {
                    selectedEndDate = selectedDate
                    btnEndDate.text = selectedDate
                }
                Log.d("BookTimeOffDentistFragment", "Selected Date: $selectedDate")
            },
            year, month, day
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = calendar.timeInMillis

        datePickerDialog.show()
    }

    private fun submitTimeOff() {
        if (selectedStartDate == null || selectedEndDate == null) {
            Toast.makeText(requireContext(), "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        val reason = editTextReason.text.toString()
        if (reason.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a reason for time off", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for existing appointments
        checkForConflictingAppointments { hasConflicts ->
            if (!hasConflicts) {
                saveTimeOff(reason)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Cannot book time off - you have existing appointments during this period",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun checkForConflictingAppointments(callback: (Boolean) -> Unit) {
        if (currentDentistId.isEmpty()) {
            callback(true) // Prevent booking if no dentist ID
            return
        }

        val appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments")
        appointmentsRef.orderByChild("dentistId")
            .equalTo(currentDentistId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasConflicts = false

                    // Parse start and end dates
                    val startDate = parseDateString(selectedStartDate!!)
                    val endDate = parseDateString(selectedEndDate!!)

                    if (startDate != null && endDate != null) {
                        for (appointmentSnapshot in snapshot.children) {
                            val appointmentDate = appointmentSnapshot.child("date").getValue(String::class.java)
                            if (appointmentDate != null) {
                                val appDate = parseDateString(appointmentDate)
                                if (appDate != null && isDateInRange(appDate, startDate, endDate)) {
                                    hasConflicts = true
                                    break
                                }
                            }
                        }
                    }

                    callback(hasConflicts)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("BookTimeOffDentistFragment", "Error checking appointments: ${error.message}")
                    callback(true) // Assume conflict on error to be safe
                }
            })
    }

    private fun parseDateString(dateStr: String): Date? {
        return try {
            val format = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
            format.parse(dateStr)
        } catch (e: Exception) {
            Log.e("BookTimeOffDentistFragment", "Error parsing date: $dateStr", e)
            null
        }
    }

    private fun isDateInRange(appointmentDate: Date, startDate: Date, endDate: Date): Boolean {
        // Remove time component from dates for comparison
        val calendar = Calendar.getInstance()

        calendar.time = appointmentDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val appDate = calendar.time

        calendar.time = startDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.time

        calendar.time = endDate
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.time

        return !appDate.before(start) && !appDate.after(end)
    }

    private fun saveTimeOff(reason: String) {
        val timeOffId = database.push().key
        if (timeOffId != null) {
            val timeOffDetails = mapOf(
                "timeOffId" to timeOffId,
                "startDate" to selectedStartDate,
                "endDate" to selectedEndDate,
                "reason" to reason,
                "dentistId" to currentDentistId,
                "status" to "pending",
                "createdAt" to ServerValue.TIMESTAMP
            )

            database.child(timeOffId).setValue(timeOffDetails)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Time off request submitted successfully!", Toast.LENGTH_SHORT).show()
                    clearInputs()
                }
                .addOnFailureListener { error ->
                    Log.e("BookTimeOffDentistFragment", "Failed to submit time off request: ${error.message}")
                    Toast.makeText(requireContext(), "Failed to submit time off request. Please try again.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearInputs() {
        selectedStartDate = null
        selectedEndDate = null
        btnStartDate.text = "Select Start Date"
        btnEndDate.text = "Select End Date"
        editTextReason.text.clear()
    }
}