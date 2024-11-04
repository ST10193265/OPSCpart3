package com.example.poe2.ui.notifications_client

import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUsername
import com.example.poe2.R
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsClientFragment : Fragment() {

    private var _binding: FragmentNotificationsClientBinding? = null
    private val binding get() = _binding!!
    private lateinit var ibtnHome: ImageButton
    private lateinit var apiService: ApiService
    private lateinit var notificationsAdapter: ArrayAdapter<String>
    private val notificationsList = mutableListOf<String>()
    private var fcmToken: String? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsClientBinding.inflate(inflater, container, false)

        // Initialize ApiService with context
        apiService = ApiClient.createApiService(requireContext())

        // Initialize UI elements from binding
        ibtnHome = binding.ibtnHome

        // Initialize the ListView adapter
        notificationsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            notificationsList
        )
        binding.notificationsListView.adapter = notificationsAdapter

        // Retrieve FCM token in a background thread
        fetchFcmToken()

        // Set up click listener for Home button
        ibtnHome.setOnClickListener {
           // Log.e("NotificationsClient", "Home button clicked")
            findNavController().navigate(R.id.action_nav_notifications_client_to_nav_menu_client)
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Register the broadcast receiver for real-time updates
        val intentFilter = IntentFilter("FCM_NOTIFICATION")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(notificationReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                requireContext().registerReceiver(notificationReceiver, intentFilter)
            }
           // Log.d("NotificationsClient", "Broadcast receiver registered for FCM_NOTIFICATION")
        } catch (e: Exception) {
           // Log.e("NotificationsClient", "Error registering receiver: ${e.message}")
        }
    }


    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
              //  Log.d("FCM", "Retrieved FCM Token: $fcmToken")
                fetchNotifications()
            } else {
             //   Log.e("FCM", "Failed to retrieve FCM Token")
            }
        }
    }

    private fun fetchNotifications() {
        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("jwt_token", null)
        val authToken = token?.let { "$it" }
        val userId = loggedInClientUserId

       // Log.d("NotificationsClient", "Attempting to fetch notifications...")
       // Log.d("NotificationsClient", "AuthToken: $authToken, UserId: $userId, FCM Token: $fcmToken")

        // Check for null values and log specific cases
        if (userId == null) Log.e("NotificationsClient", "UserId is null")
        if (authToken == null) Log.e("NotificationsClient", "AuthToken is null")
        if (fcmToken == null) Log.e("NotificationsClient", "FCM Token is null")

        // Proceed only if all required tokens are available
        if (userId != null && authToken != null && fcmToken != null) {
            apiService.getPatientNotifications(authToken, userId, fcmToken!!).enqueue(object : Callback<NotificationsResponse> {
                override fun onResponse(call: Call<NotificationsResponse>, response: Response<NotificationsResponse>) {
                 //   Log.d("NotificationsClient", "API Response Code: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val notifications = response.body()!!.notifications
                  //      Log.d("NotificationsClient", "Notifications fetched: ${notifications.size}")

                        notificationsList.clear()
                        notificationsList.addAll(notifications.map { it.message })
                        notificationsAdapter.notifyDataSetChanged()
                    } else if (response.code() == 404) {
                        // Show a toast message for no notifications
                        Toast.makeText(requireContext(), "No notifications", Toast.LENGTH_SHORT).show()
                  //      Log.e("NotificationsClient", "No notifications found for this patient")
                    } else {
                   //     Log.e("NotificationsClient", "Failed to fetch notifications: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {
                   // Log.e("NotificationsClient", "API call failed: ${t.message}")
                }
            })
        } else {
           // Log.e("NotificationsClient", "Cannot fetch notifications: Missing UserId, AuthToken, or FCM Token")
        }

    }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, receivedIntent: Intent) {
            val message = receivedIntent.getStringExtra("message")
            val timestamp = receivedIntent.getLongExtra("timestamp", 0L)

           // Log.d("NotificationsClientFragment", "Broadcast received with message: $message at timestamp: $timestamp")

            if (message != null) {
                notificationsList.add(message)
                notificationsAdapter.notifyDataSetChanged()
                Toast.makeText(context, "New notification received", Toast.LENGTH_SHORT).show()
                showNotification(context, message)
            } else {
              //  Log.e("NotificationsClientFragment", "Received null message in broadcast")
            }
        }
    }

    private fun showNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "notification_channel_id",
                "Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, "notification_channel_id")
            .setSmallIcon(R.drawable.redcircle) // Replace with your notification icon
            .setContentTitle("New Notification")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        try {
            requireContext().unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
          //  Log.e("NotificationsClient", "Receiver not registered: ${e.message}")
        }
    }


}

