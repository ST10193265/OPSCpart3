package com.example.opsc7312poepart2_code.ui.book_time_off

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.BookTimeOff
import com.example.poe2.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.isBiometricLogin

class BookTimeOffFragment : Fragment() {

    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnSubmit: Button
    private lateinit var btnHome: ImageButton
    private lateinit var btnCancel: Button
    private lateinit var txtStartDate: TextView
    private lateinit var txtEndDate: TextView
    private lateinit var etxtReason: EditText

    private lateinit var database: DatabaseReference

    private var selectedStartDate: String = ""
    private var selectedEndDate: String = ""

    private var dentistId: String? = LoginDentistFragment.loggedInDentistUserId

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book_time_off, container, false)

        // Initialize views
        btnStartDate = view.findViewById(R.id.btnStartDate)
        btnEndDate = view.findViewById(R.id.btnEndDate)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnHome = view.findViewById(R.id.ibtnHome)
        txtStartDate = view.findViewById(R.id.txtStartDate)
        txtEndDate = view.findViewById(R.id.txtEndDate)
        etxtReason = view.findViewById(R.id.etxtReason)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("booktimeoff")

        // Set onClick listeners
        btnStartDate.setOnClickListener { showDatePickerDialog(true) }
        btnEndDate.setOnClickListener { showDatePickerDialog(false) }
        btnSubmit.setOnClickListener { submitTimeOffRequest() }
        btnCancel.setOnClickListener { requireActivity().onBackPressed() }
        btnHome.setOnClickListener{ findNavController().navigate(R.id.action_bookTimeOffFragment_to_nav_menu_dentist)}


        return view
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                if (isStartDate) {
                    selectedStartDate = date
                    txtStartDate.text = date
                } else {
                    selectedEndDate = date
                    txtEndDate.text = date
                }
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun submitTimeOffRequest() {
        val reason = etxtReason.text.toString()
        if (selectedStartDate.isEmpty() || selectedEndDate.isEmpty() || reason.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Create BookTimeOff object
        val dentistId = LoginDentistFragment.loggedInDentistUserId
        val timeOffRequest = BookTimeOff(dentistId, selectedStartDate, selectedEndDate, reason)

        // Save to Firebase
        database.push().setValue(timeOffRequest)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Time off request submitted successfully", Toast.LENGTH_SHORT).show()
                // Optionally reset the fields
                txtStartDate.text = ""
                txtEndDate.text = ""
                etxtReason.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to submit request", Toast.LENGTH_SHORT).show()
            }
    }
    // The code above submits a time-off request to Firebase Realtime Database.
    // https://firebase.google.com/docs/database/android/start
}
