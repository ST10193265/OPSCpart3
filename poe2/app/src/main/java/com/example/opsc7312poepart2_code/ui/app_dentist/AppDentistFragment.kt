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
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.Appointments
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment
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
            Toast.makeText(requireContext(), "Selected: $appointmentString", Toast.LENGTH_SHORT).show()
            Log.d("AppDentistFragment", "onItemClick: Appointment selected with ID: $selectedAppointmentId")
        }

        binding.btnConfirm.setOnClickListener {
            Log.d("AppDentistFragment", "onClick: Confirm button pressed.")
            approveBooking()
        }

        binding.btnReschedule.setOnClickListener {
            Log.d("AppDentistFragment", "onClick: Reschedule button pressed.")
            rescheduleBooking()
        }

        binding.btnCancel.setOnClickListener {
            Log.d("AppDentistFragment", "onClick: Reschedule button pressed.")
            cancelBooking()
        }

        // Retrieve dentist ID and load appointments
        getDentistIdByUsername()

        apiService = ApiClient.createApiService(requireContext())

        return view
    }


    private fun getDentistIdByUsername() {
        Log.d("AppDentistFragment", "getDentistIdByUsername: Attempting to retrieve dentist ID from LoginDentistFragment.")

        // Attempt to retrieve dentist ID from LoginDentistFragment
        dentistId = LoginDentistFragment.loggedInDentistUserId

        if (dentistId != null) {
            Log.d("AppDentistFragment", "getDentistIdByUsername: Successfully retrieved Dentist ID: $dentistId")
            fetchAppointments()
        } else {
            Log.e("AppDentistFragment", "getDentistIdByUsername: No logged-in dentist ID found.")
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAppointments() {
        if (dentistId == null) {
            Log.e("AppDentistFragment", "fetchAppointments: Dentist ID is null, cannot fetch appointments.")
            return
        }

        Log.d("AppDentistFragment", "fetchAppointments: Fetching appointments for dentist ID: $dentistId")

        appointmentsDatabase.orderByChild("dentistId").equalTo(dentistId)
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
        if (selectedAppointmentId == null) {
            Toast.makeText(requireContext(), "No appointment selected", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("AppDentistFragment", "approveBooking: Approving booking for appointment ID: $selectedAppointmentId")

        // Call the API to approve the appointment using appointmentId
        apiService.approveAppointment(selectedAppointmentId!!, Appointments("approved")) // Assuming you want to update the status
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
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


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("AppDentistFragment", "onDestroyView: Cleaning up binding.")
        _binding = null
    }
}
