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
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.google.firebase.messaging.FirebaseMessaging
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
    private val SERVER_KEY = "BLkQ8flFlwMwiVw93qmEpY6vYNxi95ri8NX58i63-xwuF77k_qqd6PmJ7j4H9I-H2VqS02IBtHO2f3zYT1ttZzw"
    private var fcmToken: String? = null // Store FCM token here

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

        // Retrieve FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                Log.d("FCM", "Retrieved FCM Token: $fcmToken")
                // Optionally, you can send the token to the server here
                // sendFCMTokenToServer(fcmToken)
            } else {
                Log.e("FCM", "Failed to retrieve FCM Token")
            }
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch notifications from API when the fragment opens
        fetchNotifications()

        // Register the broadcast receiver for real-time updates
        val intentFilter = IntentFilter("FCM_NOTIFICATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(notificationReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(notificationReceiver, intentFilter)
        }
    }

    private fun fetchNotifications() {
        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("jwt_token", null)
        Log.d("TokenDebug", "Token retrieved: $token")

        // Prefix with Bearer if necessary
        val authToken = token?.let { "Bearer $it" }
        val userId = loggedInClientUserId
        Log.d("NotificationsClient", "UserId: $userId")
        Log.d("NotificationsClient", "AuthToken: $authToken")

        if (userId != null && authToken != null) {
            fcmToken?.let {
                apiService.getPatientNotifications(authToken, userId, it).enqueue(object : Callback<NotificationsResponse> {
                    override fun onResponse(call: Call<NotificationsResponse>, response: Response<NotificationsResponse>) {
                        Log.d("NotificationsClient", "Response Code: ${response.code()}")
                        Log.d("NotificationsClient", "Response Body: ${response.body()}")

                        if (response.isSuccessful && response.body() != null) {
                            notificationsList.clear()
                            notificationsList.addAll(response.body()!!.notifications.map { it.message })
                            notificationsAdapter.notifyDataSetChanged()
                        } else {
                            Log.e("NotificationsClient", "Failed to fetch notifications: ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {
                        Log.e("NotificationsClient", "API call failed: ${t.message}")
                        t.printStackTrace()
                    }
                })
            }
        } else {
            Log.e("NotificationsClient", "UserId or AuthToken is null")
        }
    }


    // Function to send push notifications (if needed)
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
        val data = JSONObject().apply {
            put("to", token)
            put("notification", JSONObject().apply {
                put("title", "New Notification")
                put("body", message)
            })
        }

        // Create the JsonObjectRequest
        val request = object : JsonObjectRequest(
            Request.Method.POST,
            FCM_URL,
            data,
            { response ->
                Log.d("FCM", "Notification sent successfully: $response")
            },
            { error ->
                Log.e("FCM", "Error sending notification: ${error.message}")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "key=$SERVER_KEY"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

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
        try {
            requireContext().unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("NotificationsClient", "Receiver not registered: ${e.message}")
        }
    }
}

