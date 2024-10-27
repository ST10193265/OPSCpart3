package com.example.poe2.ui.notifications_client

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.Notification
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.example.poe2.R
import com.example.poe2.databinding.FragmentNotificationsClientBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse

class NotificationsClientFragment : Fragment() {

    private var _binding: FragmentNotificationsClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var apiService: ApiService
    private lateinit var btnViewNotifications: Button
    private lateinit var notificationsAdapter: ArrayAdapter<String>
    private val notificationsList = mutableListOf<String>()

    // Flag to check if notifications have been loaded
    private var notificationsLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsClientBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize ApiService with context
        apiService = ApiClient.createApiService(requireContext())

        // Initialize the ListView adapter
        notificationsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            notificationsList
        )
        binding.notificationsListView.adapter = notificationsAdapter

        // Initialize button and set up the click listener
        btnViewNotifications = binding.btnViewNotifications
        btnViewNotifications.setOnClickListener {
            Log.d("NotificationsClient", "View notifications button clicked")
            // Load notifications only if they haven't been loaded yet
            if (!notificationsLoaded) {
                loadNotifications()
            } else {
                Toast.makeText(context, "Notifications already loaded", Toast.LENGTH_SHORT).show()
            }
        }

        // Home button navigation
        binding.ibtnHome.setOnClickListener {
            Log.d("NotificationsClient", "Home button clicked")
            findNavController().navigate(R.id.action_nav_notifications_client_to_nav_menu_client)
        }

        return view
    }

    private fun loadNotifications() {
        Log.d("NotificationsClient", "Starting to load notifications")

        // Log the token before making the API call
        val token = ApiClient.getTokenFromSharedPreferences(requireContext())
        Log.d("NotificationsClient", "Using token for notifications: $token")

        // Use Coroutine for asynchronous execution
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getPatientNotifications().awaitResponse()
                withContext(Dispatchers.Main) {
                    Log.d("NotificationsClient", "API Response Code: ${response.code()}") // Log response code
                    if (response.isSuccessful && response.body() != null) {
                        val apiResponse = response.body()!!
                        notificationsList.clear()

                        // Add each notification's message to the list
                        apiResponse.notifications.forEach { notification ->
                            notificationsList.add(notification.message)
                            Log.d("NotificationsClient", "Added notification: ${notification.message}")
                        }

                        // Notify the adapter of data changes
                        notificationsAdapter.notifyDataSetChanged()

                        // Check if there are no notifications
                        if (notificationsList.isEmpty()) {
                            Toast.makeText(context, "No notifications available", Toast.LENGTH_SHORT).show()
                        } else {
                            // Set the flag to true indicating notifications are loaded
                            notificationsLoaded = true
                        }
                    } else {
                        // Handle error response
                        val error = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("NotificationsClient", "Failed to load notifications: $error")
                        Toast.makeText(context, "Failed to load notifications: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Handle request failure
                Log.e("NotificationsClient", "Failed to load notifications: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load notifications: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear the binding reference to avoid memory leaks
    }
}





