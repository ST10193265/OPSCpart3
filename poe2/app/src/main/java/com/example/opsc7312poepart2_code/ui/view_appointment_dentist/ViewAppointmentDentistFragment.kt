package com.example.poe2.ui.book_appointment_dentist

import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.loggedInDentistUserId
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

class ViewAppointmentDentistFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var appointmentListView: ListView
    private lateinit var database: DatabaseReference

    private var dentistAppointments = mutableMapOf<String, List<String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_view_appointment_dentist, container, false)

        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_view_appointment_dentist_to_nav_menu_dentist)
        }

        calendarView = view.findViewById(R.id.calendarView1)
        appointmentListView = view.findViewById(R.id.appointmentListView1)

        database = FirebaseDatabase.getInstance().getReference("appointments")

        val dentistId = loggedInDentistUserId

        if (dentistId != null) {
            loadDentistAppointments(dentistId)
        } else {
            Toast.makeText(requireContext(), "Dentist not logged in.", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadDentistAppointments(dentistId: String) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dentistAppointments.clear()

                for (appointmentSnapshot in snapshot.children) {
                    val appointmentDentistId = appointmentSnapshot.child("dentistId").getValue(String::class.java)

                    if (appointmentDentistId == dentistId) {
                        val appointmentDate = appointmentSnapshot.child("date").getValue(String::class.java) ?: continue
                        val appointmentTime = appointmentSnapshot.child("slot").getValue(String::class.java) ?: continue
                        val appointmentDesc = appointmentSnapshot.child("description").getValue(String::class.java) ?: ""

                        val appointmentDetails = "$appointmentTime - $appointmentDesc"
                        dentistAppointments[appointmentDate] = dentistAppointments.getOrDefault(appointmentDate, listOf()) + appointmentDetails
                    }
                }

                highlightAppointmentDays()

                calendarView.setOnDayClickListener(object : OnDayClickListener {
                    override fun onDayClick(eventDay: EventDay) {
                        val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
                        val selectedDate = dateFormat.format(eventDay.calendar.time)
                        Log.d("ViewAppointmentDentistFragment", "Selected date: $selectedDate")

                        val appointmentList = dentistAppointments[selectedDate]

                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_list_item_1,
                            appointmentList ?: listOf("No appointments")
                        )
                        appointmentListView.adapter = adapter
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
             //   Log.e("ViewAppointmentDentistFragment", "Failed to load appointments: ${error.message}")
                Toast.makeText(requireContext(), "Failed to load appointments.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun highlightAppointmentDays() {
        // Parse and log dates with appointments to ensure they are processed correctly
        val datesWithAppointments = dentistAppointments.keys.mapNotNull { dateString ->
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            try {
                val date = dateFormat.parse(dateString)
                Calendar.getInstance().apply { time = date } // Log each parsed date
            } catch (e: ParseException) {
             //   Log.e("ViewAppointmentDentistFragment", "Invalid date format: $dateString")
                null
            }
        }

        // Create and log EventDay objects to confirm correct setup
        val events = datesWithAppointments.map { calendar ->
          //  Log.d("ViewAppointmentDentistFragment", "Adding event on: ${calendar.time}")
            EventDay(calendar, R.drawable.redcircle, Color.RED)
        }

        // Apply events to CalendarView and verify that events are being set
        calendarView.setEvents(events)
      //  Log.d("ViewAppointmentDentistFragment", "Events set for dates with appointments.")
    }

}