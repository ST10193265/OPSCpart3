package com.example.opsc7312poepart2_code.ui.app_dentist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.opsc7312poepart2_code.ui.Appointments
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class AppDentistViewModel : ViewModel() {
    private val _appointments = MutableLiveData<List<Appointments>>()
    val appointments: LiveData<List<Appointments>> get() = _appointments

    fun loadAppointmentsForUser(dentistId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("appointments")

        // Query appointments for the specific dentist by dentistId
        databaseReference.orderByChild("dentistId").equalTo(dentistId) // Fetch based on dentist ID
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val appointmentsList = mutableListOf<Appointments>()
                    for (appointmentSnapshot in dataSnapshot.children) {
                        val appointment = appointmentSnapshot.getValue(Appointments::class.java)
                        appointment?.let { appointmentsList.add(it) }
                    }
                    _appointments.value = appointmentsList
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle any errors here
                    _appointments.value = emptyList()
                }
            })
    }
}