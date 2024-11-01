package com.example.poe2.ui.notifications_dentist

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.RequiresApi
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.NotificationsResponse
import com.example.poe2.databinding.FragmentNotificationsDentistBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsDentistFragment : Fragment() {

    private var _binding: FragmentNotificationsDentistBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var apiService: ApiService
    private lateinit var notificationsAdapter: ArrayAdapter<String>
    private val notificationsList = mutableListOf<String>()

    private val FCM_URL = "https://fcm.googleapis.com/fcm/send"
    private val SERVER_KEY = "YOUR_SERVER_KEY" // Replace with your actual FCM server key
    private val notificationPermissionRequestCode = 1001

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsDentistBinding.inflate(inflater, container, false)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("appointments")

        // Initialize ApiService with context
        apiService = ApiClient.createApiService(requireContext())

        // Initialize the ListView adapter
        notificationsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            notificationsList
        )
        binding.appointmentsListView.adapter = notificationsAdapter

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch notifications from API when the fragment opens
        fetchNotifications()

        // Register the broadcast receiver for real-time updates
        requireContext().registerReceiver(
            notificationReceiver,
            IntentFilter("FCM_NOTIFICATION"),
            Context.RECEIVER_NOT_EXPORTED
        )

        // Start listening for new appointments
        listenForNewAppointments(getLoggedInDentistId())
    }

    // Function to fetch notifications
    private fun fetchNotifications() {
        apiService.getDentistNotifications()?.enqueue(object : Callback<NotificationsResponse> {
            override fun onResponse(
                call: Call<NotificationsResponse>,
                response: Response<NotificationsResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    notificationsList.clear()
                    notificationsList.addAll(response.body()!!.notifications.map { it.message })
                    notificationsAdapter.notifyDataSetChanged()
                } else {
                    Log.e("NotificationsDentist", "Failed to fetch notifications")
                }
            }

            override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {
                Log.e("NotificationsDentist", "API call failed: ${t.message}")
            }
        })
    }

    private fun listenForNewAppointments(dentistId: String?) {
        dentistId ?: return
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val appointmentDentistId = snapshot.child("dentistId").getValue(String::class.java)
                if (appointmentDentistId == dentistId) {
                    val appointmentDate = snapshot.child("date").getValue(String::class.java) ?: "N/A"
                    val appointmentSlot = snapshot.child("slot").getValue(String::class.java) ?: "N/A"
                    val appointmentDescription = snapshot.child("description").getValue(String::class.java) ?: "N/A"

                    val appointmentDetails = "Date: $appointmentDate, Slot: $appointmentSlot, Description: $appointmentDescription"

                    // Add the new appointment to the list and notify the adapter
                    notificationsList.add(appointmentDetails)
                    notificationsAdapter.notifyDataSetChanged()

                    // Send push notification to dentist
                    sendPushNotification(appointmentDetails)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle changes in appointments if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle removal of appointments if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle moved appointments if needed
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationsDentistFragment", "Failed to listen for appointments: ${error.message}")
            }
        })
    }

    private fun sendPushNotification(message: String) {
        val fcmToken = getDentistFCMToken() // Get the FCM token for the logged-in dentist
        if (fcmToken != null) {
            sendFCMMessage(fcmToken, message)
        } else {
            Log.e("NotificationsDentist", "FCM Token not found")
        }
    }

    private fun sendFCMMessage(token: String, message: String) {
        val data = JSONObject()
        data.put("to", token)
        data.put("notification", JSONObject().apply {
            put("title", "New Appointment")
            put("body", message)
        })

        // Create the JsonObjectRequest
        val request = object : JsonObjectRequest(
            Request.Method.POST,
            FCM_URL,
            data,
            { response ->
                // Success listener
                Log.d("FCM", "Notification sent successfully: $response")
            },
            { error ->
                // Error listener
                Log.e("FCM", "Error sending notification: ${error.message}")
            }
        ) {
            // Add the required headers for FCM
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "key=$SERVER_KEY" // Use your FCM server key
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        // Add the request to the RequestQueue
        Volley.newRequestQueue(requireContext()).add(request)
    }

    private fun getLoggedInDentistId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun getDentistFCMToken(): String? {
        // Replace this with your method to fetch the FCM token for the dentist
        return "DENTIST_FCM_TOKEN" // Placeholder, implement your logic here
    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("message")
            if (message != null) {
                notificationsList.add(message)
                notificationsAdapter.notifyDataSetChanged()
                Toast.makeText(context, "New notification received", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireContext().unregisterReceiver(notificationReceiver)
    }
}

