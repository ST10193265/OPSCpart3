package com.example.opsc7312poepart2_code.ui.book_time_off

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.isBiometricLogin
import com.example.poe2.R
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class BookTimeOffFragment : Fragment() {

    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var txtStartDate: TextView
    private lateinit var txtEndDate: TextView
    private lateinit var etxtReason: EditText
    private lateinit var btnSubmit: Button // Submit button
    private lateinit var btnCancel: Button // Cancel button

    private var startDateMillis: Long = 0
    private var endDateMillis: Long = 0
    private var dentistId: String? = LoginDentistFragment.loggedInDentistUserId // Get the dentist ID from the LoginDentistFragment

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book_time_off, container, false)

        btnStartDate = view.findViewById(R.id.btnStartDate)
        btnEndDate = view.findViewById(R.id.btnEndDate)
        txtStartDate = view.findViewById(R.id.txtStartDate)
        txtEndDate = view.findViewById(R.id.txtEndDate)
        etxtReason = view.findViewById(R.id.etxtReason)
        btnSubmit = view.findViewById(R.id.btnSubmit) // Initialize submit button
        btnCancel = view.findViewById(R.id.btnCancel) // Initialize cancel button

        btnStartDate.setOnClickListener { showDatePickerDialog(true) }
        btnEndDate.setOnClickListener { showDatePickerDialog(false) }
        btnSubmit.setOnClickListener { submitTimeOff() } // Handle submission
        btnCancel.setOnClickListener { clearFields() } // Handle cancel

        // If logged in using biometrics, set the dentistId accordingly
        if (isBiometricLogin) {
            dentistId = LoginDentistFragment.loggedInDentistUserId // Ensure dentistId is available
        }

        return view
    }

    @SuppressLint("StringFormatInvalid")
    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)

            if (isStartDate) {
                startDateMillis = selectedDate.timeInMillis
                txtStartDate.text = getString(R.string.date, dayOfMonth, month + 1, year)
            } else {
                endDateMillis = selectedDate.timeInMillis
                txtEndDate.text = getString(R.string.date, dayOfMonth, month + 1, year)
            }

            validateDates()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }

    private fun validateDates() {
        // Prevent booking if the start date is after the end date
        if (startDateMillis > endDateMillis && endDateMillis != 0L) {
            etxtReason.error = "Start date must be before end date."
        } else {
            etxtReason.error = null // Clear error if valid
        }
    }

    private fun submitTimeOff() {
        // Collect the data
        val reason = etxtReason.text.toString()
        if (startDateMillis == 0L || endDateMillis == 0L || reason.isEmpty()) {
            // Show error if dates or reason are invalid
            if (reason.isEmpty()) etxtReason.error = "Please provide a reason."
            return
        }

        // Prepare the booking data, including the dentist ID
        val bookingData = mapOf(
            "dentistId" to dentistId, // Use dentist ID from LoginDentistFragment
            "startDate" to startDateMillis,
            "endDate" to endDateMillis,
            "reason" to reason
        )

        // Reference to Firebase Realtime Database
        val database = FirebaseDatabase.getInstance().reference
        database.child("timeOffBookings").push().setValue(bookingData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Handle success, e.g., show a success message
                    Toast.makeText(context, "Time off booked successfully", Toast.LENGTH_SHORT).show()
                    clearFields() // Clear fields after successful booking
                } else {
                    // Handle failure
                    Toast.makeText(context, "Failed to book time off", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun clearFields() {
        // Clear all input fields and reset dates
        startDateMillis = 0
        endDateMillis = 0
        txtStartDate.text = ""
        txtEndDate.text = ""
        etxtReason.text.clear()
        etxtReason.error = null // Clear any error messages
    }
}
