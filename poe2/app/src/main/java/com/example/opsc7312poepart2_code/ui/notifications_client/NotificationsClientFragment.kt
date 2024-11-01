package com.example.poe2.ui.notifications_client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.poe2.databinding.FragmentNotificationsClientBinding
import com.example.opsc7312poepart2_code.ui.Notification
import com.example.opsc7312poepart2_code.ui.NotificationsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsClientFragment : Fragment() {

    private var _binding: FragmentNotificationsClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var apiService: ApiService
    private lateinit var notificationsAdapter: ArrayAdapter<String>
    private val notificationsList = mutableListOf<String>()
    private val FCM_URL = "https://fcm.googleapis.com/fcm/send"
    private val SERVER_KEY = "YOUR_SERVER_KEY" // Replace with your actual FCM server key

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsClientBinding.inflate(inflater, container, false)

        // Initialize ApiService with context
        apiService = ApiClient.createApiService(requireContext())

        // Initialize the ListView adapter
        notificationsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            notificationsList
        )
        binding.notificationsListView.adapter = notificationsAdapter

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
    }

    // Fetch notifications from the API
    private fun fetchNotifications() {
        apiService.getPatientNotifications().enqueue(object : Callback<NotificationsResponse> {
            override fun onResponse(
                call: Call<NotificationsResponse>,
                response: Response<NotificationsResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    notificationsList.clear()
                    notificationsList.addAll(response.body()!!.notifications.map { it.message })
                    notificationsAdapter.notifyDataSetChanged()

                    // Send push notifications for each notification received
                    sendPushNotifications(response.body()!!.notifications)
                } else {
                    Log.e("NotificationsClient", "Failed to fetch notifications")
                }
            }

            override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {
                Log.e("NotificationsClient", "API call failed: ${t.message}")
            }
        })
    }

    // Function to send push notifications
    private fun sendPushNotifications(notifications: List<Notification>) {
        // Iterate through each notification and send a push notification
        for (notification in notifications) {
            val message = notification.message
            val fcmToken = notification.fcmToken
            // Call your method to send notification via FCM
            if (fcmToken != null) {
                sendFCMMessage(fcmToken, message)
            }
        }
    }

    // Function to send a message via FCM
    private fun sendFCMMessage(token: String, message: String) {
        val data = JSONObject()
        data.put("to", token)
        data.put("notification", JSONObject().apply {
            put("title", "New Notification")
            put("body", message)
        })

        // Create the JsonObjectRequest
        val request = object : JsonObjectRequest(
            Request.Method.POST,
            FCM_URL,
            data,
            { response ->
                // This is the success listener
                Log.d("FCM", "Notification sent successfully: $response")
            },
            { error ->
                // This is the error listener
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

