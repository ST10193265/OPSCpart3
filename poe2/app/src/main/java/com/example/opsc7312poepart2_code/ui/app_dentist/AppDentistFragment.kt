package com.example.opsc7312poepart2_code.ui.app_dentist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.Appointments
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.isBiometricLogin
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.loggedInDentistUserId
import com.example.poe2.R
import com.example.poe2.databinding.FragmentAppDentistBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.firebase.database.*

class AppDentistFragment : Fragment() {

    private lateinit var appointmentsDatabase: DatabaseReference
    private var dentistId: String? = null

    private lateinit var appointmentList: MutableList<Appointments>
    private lateinit var appointmentIdList: MutableList<String> // List to store appointment IDs
    private lateinit var appointmentAdapter: ArrayAdapter<String>
    private var _binding: FragmentAppDentistBinding? = null
    private val binding get() = _binding!!

    private lateinit var apiService: ApiService

    private var selectedAppointmentId: String? = null // Store the selected appointment ID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAppDentistBinding.inflate(inflater, container, false)
        val view = binding.root

        Log.d("AppDentistFragment", "onCreateView: Initializing Firebase database reference and ListView.")

        // Initialize Firebase Database reference
        appointmentsDatabase = FirebaseDatabase.getInstance().getReference("appointments")

        // Initialize Lists and adapter
        appointmentList = mutableListOf()
        appointmentIdList = mutableListOf() // Initialize appointment ID list
        appointmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.listAppointments.adapter = appointmentAdapter

        // Set up item click listener for ListView
        binding.listAppointments.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            // Get the selected appointment ID from the appointmentIdList
            selectedAppointmentId = appointmentIdList[position] // Get the ID from the clicked item
            val appointmentString = appointmentAdapter.getItem(position)

            Log.d("AppDentistFragment", "onItemClick: Appointment selected with ID: $selectedAppointmentId")
        }

        binding.btnConfirm.setOnClickListener {
            if (isBiometricLogin)
            {
                approveBookingBio()
            }
            else
            {
                approveBooking()
            }

            Log.d("AppDentistFragment", "onClick: Confirm button pressed.")

        }

        binding.btnReschedule.setOnClickListener {

            if (isBiometricLogin)
            {
                rescheduleBookingBio()
            }
            else
            {
                rescheduleBooking()
            }
            Log.d("AppDentistFragment", "onClick: Reschedule button pressed.")

        }

        binding.btnCancel.setOnClickListener {
            if (isBiometricLogin)
            {
                cancelBookingBio()
            }
            else
            {
                cancelBooking()
            }
            Log.d("AppDentistFragment", "onClick: Reschedule button pressed.")

        }

        binding.ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dentist_app_to_nav_menu_dentist)
            Log.d("AppDentistFragment", "onClick: home button pressed.")
        }

        // Retrieve dentist ID and load appointments
        getDentistIdByUsername()

        apiService = ApiClient.createApiService(requireContext())

        return view
    }


    private fun getDentistIdByUsername() {
        val loggedInUsername = LoginDentistFragment.loggedInDentistUsername // Assuming this stores the current dentist's username

        Log.d("AppDentistFragment", "getDentistIdByUsername: username for user: $loggedInUsername")
        Log.d("AppDentistFragment", "getDentistIdByUsername:  ID for user: $loggedInDentistUserId")

        if (loggedInUsername.isNullOrEmpty()) {
            Log.e("AppDentistFragment", "getDentistIdByUsername: No logged-in dentist username found.")
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to Firebase users table
        val dbReference = FirebaseDatabase.getInstance().getReference("appointments")

        dbReference.orderByChild("dentistId").equalTo(loggedInDentistUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("AppDentistFragment", "$snapshot")

                    if (snapshot.exists()) {
                       // val userSnapshot = snapshot.children.firstOrNull()



                        Log.d("AppDentistFragment", "getDentistIdByUsername: Successfully retrieved Dentist ID: $loggedInDentistUserId")
                        fetchAppointments() // Call to fetch appointments after ID retrieval
                    } else {
                        Log.e("AppDentistFragment", "getDentistIdByUsername: Dentist ID not found for username: $loggedInUsername")
                        Toast.makeText(requireContext(), "Dentist not found in database", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppDentistFragment", "getDentistIdByUsername: Database error: ${error.message}")
                    Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun fetchAppointments() {
        if (loggedInDentistUserId == null) {
            Log.e("AppDentistFragment", "fetchAppointments: Dentist ID is null, cannot fetch appointments.")
            return
        }

        Log.d("AppDentistFragment", "fetchAppointments: Fetching appointments for dentist ID: $loggedInDentistUserId")

        appointmentsDatabase.orderByChild("dentistId").equalTo(loggedInDentistUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("AppDentistFragment", "onDataChange: Retrieved ${snapshot.childrenCount} appointments from database.")
                    appointmentAdapter.clear() // Clear previous data
                    appointmentList.clear() // Clear previous appointments from the list
                    appointmentIdList.clear() // Clear previous appointment IDs

                    for (appointmentSnapshot in snapshot.children) {
                        val appointment = appointmentSnapshot.getValue(Appointments::class.java)
                        if (appointment != null && appointment.status == "pending") {
                            // Use the key of the snapshot as appointmentId
                            val appointmentId = appointmentSnapshot.key

                            Log.d("AppDentistFragment", "onDataChange: Adding appointment: $appointment with ID: $appointmentId")
                            // Format the appointment details
                            val displayString = """
                                Date: ${appointment.date}
                                Client: ${appointment.clientUsername}
                                Description: ${appointment.description}
                                Status: ${appointment.status}
                            """.trimIndent()

                            appointmentAdapter.add(displayString)
                            appointmentList.add(appointment) // Store the actual appointment object
                            appointmentIdList.add(appointmentId!!) // Store the ID for later use
                        } else if (appointment == null) {
                            Log.w("AppDentistFragment", "onDataChange: Found an invalid appointment entry.")
                        }
                    }
                    appointmentAdapter.notifyDataSetChanged()
                    Log.d("AppDentistFragment", "onDataChange: Appointments list updated in ListView.")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AppDentistFragment", "onCancelled: Database error: ${error.message}")
                    Toast.makeText(requireContext(), "Failed to load appointments", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun approveBooking() {
       // Toast.makeText(requireContext(), "${selectedAppointmentId}", Toast.LENGTH_SHORT).show()
        if (selectedAppointmentId == null) {
           Toast.makeText(requireContext(), "No appointment selected", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("AppDentistFragment", "approveBooking: Approving booking for appointment ID: $selectedAppointmentId")

        // Call the API to approve the appointment using appointmentId
        apiService.approveAppointment(selectedAppointmentId.toString(), Appointments("approved") ) // Assuming you want to update the status
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                    Log.d("AppDentistFragment", " response = $response")
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Booking approved successfully", Toast.LENGTH_SHORT).show()
                        fetchAppointments()
                    } else {
                        Log.e("AppDentistFragment", "approveBooking: Failed to approve booking. Response code: ${response.code()}, message: ${response.message()}")
                        response.errorBody()?.string()?.let { errorBody ->
                            Log.e("AppDentistFragment", "Error body: $errorBody")
                        }
                        Toast.makeText(requireContext(), "Failed to approve booking: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("AppDentistFragment", "approveBooking: API call failed: ${t.message}")
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun rescheduleBooking() {
        if (selectedAppointmentId == null) {
            Toast.makeText(requireContext(), "No appointment selected", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("AppDentistFragment", "rescheduleBooking: Rescheduling booking for appointment ID: $selectedAppointmentId")

        // Create an Appointments object with the status set to "rescheduled"
        val updatedAppointment = Appointments(status = "rescheduled") // Update the status

        // Call the API to reschedule the appointment using appointmentId
        apiService.rescheduleAppointment(selectedAppointmentId!!, updatedAppointment) // Ensure your API method can handle this
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Booking rescheduled successfully", Toast.LENGTH_SHORT).show()
                        fetchAppointments()
                    } else {
                        Log.e("AppDentistFragment", "rescheduleBooking: Failed to reschedule booking. Response code: ${response.code()}, message: ${response.message()}")
                        response.errorBody()?.string()?.let { errorBody ->
                            Log.e("AppDentistFragment", "Error body: $errorBody")
                        }
                        Toast.makeText(requireContext(), "Failed to reschedule booking: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("AppDentistFragment", "rescheduleBooking: API call failed: ${t.message}")
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun cancelBooking() {
        if (selectedAppointmentId == null) {
            Toast.makeText(requireContext(), "No appointment selected", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("AppDentistFragment", "cancelBooking: Canceling booking for appointment ID: $selectedAppointmentId")

        // Create an Appointments object with the status set to "cancel"
        val updatedAppointment = Appointments(status = "cancel") // Update the status to "cancel"

        // Call the API to cancel the appointment using appointmentId
        apiService.cancelAppointment(selectedAppointmentId!!, updatedAppointment) // Ensure your API method can handle this
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Booking canceled successfully", Toast.LENGTH_SHORT).show()
                        fetchAppointments()
                    } else {
                        Log.e("AppDentistFragment", "cancelBooking: Failed to cancel booking. Response code: ${response.code()}, message: ${response.message()}")
                        response.errorBody()?.string()?.let { errorBody ->
                            Log.e("AppDentistFragment", "Error body: $errorBody")
                        }
                        Toast.makeText(requireContext(), "Failed to cancel booking: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("AppDentistFragment", "cancelBooking: API call failed: ${t.message}")
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun approveBookingBio() {
        if (selectedAppointmentId == null) {
            Toast.makeText(requireContext(), "No appointment selected", Toast.LENGTH_SHORT).show()
            return
        }

        val appointmentId = selectedAppointmentId.toString()
        val appointmentRef = FirebaseDatabase.getInstance().getReference("appointments/$appointmentId")

        // Check if the appointment exists and its current status
        appointmentRef.get().addOnSuccessListener { appointmentSnapshot ->
            if (!appointmentSnapshot.exists()) {
                Toast.makeText(requireContext(), "Appointment not found", Toast.LENGTH_SHORT).show()
                Log.e("AppDentistFragment", "approveBooking: Appointment not found for ID: $appointmentId")
                return@addOnSuccessListener
            }

            val currentStatus = appointmentSnapshot.child("status").value as? String
            when (currentStatus) {
                "approved" -> {
                    Toast.makeText(requireContext(), "This appointment is already confirmed", Toast.LENGTH_SHORT).show()
                }
                "canceled" -> {
                    Toast.makeText(requireContext(), "This appointment has been canceled and cannot be approved", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Update the status to 'approved'
                    val updates = mapOf(
                        "status" to "approved",
                        "updatedAt" to ServerValue.TIMESTAMP
                    )
                    appointmentRef.updateChildren(updates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Booking approved successfully", Toast.LENGTH_SHORT).show()
                            Log.d("AppDentistFragment", "approveBooking: Successfully approved booking for appointment ID: $appointmentId")
                            fetchAppointments() // Refresh appointments list
                        } else {
                            Toast.makeText(requireContext(), "Failed to approve booking", Toast.LENGTH_SHORT).show()
                            Log.e("AppDentistFragment", "approveBooking: Failed to update appointment status")
                        }
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            Log.e("AppDentistFragment", "approveBooking: Error fetching appointment - ${exception.message}")
        }
    }

    private fun rescheduleBookingBio() {
        if (selectedAppointmentId == null) {
            Toast.makeText(requireContext(), "No appointment selected", Toast.LENGTH_SHORT).show()
            return
        }

        val appointmentId = selectedAppointmentId.toString()
        val appointmentRef = FirebaseDatabase.getInstance().getReference("appointments/$appointmentId")

        // Check if the appointment exists and its current status
        appointmentRef.get().addOnSuccessListener { appointmentSnapshot ->
            if (!appointmentSnapshot.exists()) {
                Toast.makeText(requireContext(), "Appointment not found", Toast.LENGTH_SHORT).show()
                Log.e("AppDentistFragment", "approveBooking: Appointment not found for ID: $appointmentId")
                return@addOnSuccessListener
            }

            val currentStatus = appointmentSnapshot.child("status").value as? String
            when (currentStatus) {
                "reschedule" -> {
                    Toast.makeText(requireContext(), "This appointment is already rescheduled", Toast.LENGTH_SHORT).show()
                }
                "canceled" -> {
                    Toast.makeText(requireContext(), "This appointment has been canceled and cannot be approved", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Update the status to 'approved'
                    val updates = mapOf(
                        "status" to "reschedule",
                        "updatedAt" to ServerValue.TIMESTAMP
                    )
                    appointmentRef.updateChildren(updates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Booking rescheduled successfully", Toast.LENGTH_SHORT).show()
                            Log.d("AppDentistFragment", "approveBooking: Successfully approved booking for appointment ID: $appointmentId")
                            fetchAppointments() // Refresh appointments list
                        } else {
                            Toast.makeText(requireContext(), "Failed to reschedule booking", Toast.LENGTH_SHORT).show()
                            Log.e("AppDentistFragment", "approveBooking: Failed to update appointment status")
                        }
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            Log.e("AppDentistFragment", "approveBooking: Error fetching appointment - ${exception.message}")
        }
    }

    private fun cancelBookingBio() {
        if (selectedAppointmentId == null) {
            Toast.makeText(requireContext(), "No appointment selected", Toast.LENGTH_SHORT).show()
            return
        }

        val appointmentId = selectedAppointmentId.toString()
        val appointmentRef = FirebaseDatabase.getInstance().getReference("appointments/$appointmentId")

        // Check if the appointment exists and its current status
        appointmentRef.get().addOnSuccessListener { appointmentSnapshot ->
            if (!appointmentSnapshot.exists()) {
                Toast.makeText(requireContext(), "Appointment not found", Toast.LENGTH_SHORT).show()
                Log.e("AppDentistFragment", "approveBooking: Appointment not found for ID: $appointmentId")
                return@addOnSuccessListener
            }

            val currentStatus = appointmentSnapshot.child("status").value as? String
            when (currentStatus) {
                "cancel" -> {
                    Toast.makeText(requireContext(), "This appointment is already canceled", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    // Update the status to 'approved'
                    val updates = mapOf(
                        "status" to "cancel",
                        "updatedAt" to ServerValue.TIMESTAMP
                    )
                    appointmentRef.updateChildren(updates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Booking canceled successfully", Toast.LENGTH_SHORT).show()
                            Log.d("AppDentistFragment", "approveBooking: Successfully approved booking for appointment ID: $appointmentId")
                            fetchAppointments() // Refresh appointments list
                        } else {
                            Toast.makeText(requireContext(), "Failed to cancel booking", Toast.LENGTH_SHORT).show()
                            Log.e("AppDentistFragment", "approveBooking: Failed to update appointment status")
                        }
                    }
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            Log.e("AppDentistFragment", "approveBooking: Error fetching appointment - ${exception.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AppDentistFragment", "onDestroyView: Cleaning up binding.")
        _binding = null
    }
}
