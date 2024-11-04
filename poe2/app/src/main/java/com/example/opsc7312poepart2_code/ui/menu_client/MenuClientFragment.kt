package com.example.poe2.ui.menu_client

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.Notification
import com.example.opsc7312poepart2_code.ui.NotificationsResponse
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUsername

import com.example.poe2.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MenuClientFragment : Fragment() {

    private lateinit var txtNotificationCount: TextView
    private val notificationsList = mutableListOf<String>()
    private lateinit var apiService: ApiService
    private var fcmToken: String? = null
    private lateinit var notificationsAdapter: ArrayAdapter<String>
    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_menu_client, container, false)

        // Initialize Firebase database here
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("clients")

        // Initialize ApiService with context
        apiService = ApiClient.createApiService(requireContext())

        // Initialize UI components
        setupUI(view)

        val username = loggedInClientUsername
        if (username != null) {
            getUserIdFromFirebase(username)
        }



        return view
    }

    private fun getUserIdFromFirebase(username: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    loggedInClientUserId = snapshot.children.first().key // Get user ID
                    Log.d("LoginClientFragment", "Logged in user ID: $loggedInClientUserId")
                    // Retrieve FCM token in a background thread
                    fetchFcmToken()
                } else {
                    Log.d("LoginClientFragment", "User ID not found for $username")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginClientFragment", "Database error when retrieving user ID: ${error.message}")
            }
        })
    }

    private fun setupUI(view: View) {
        txtNotificationCount = view.findViewById(R.id.txtNotificationCount)

        val ibtnSettings: ImageButton = view.findViewById(R.id.ibtnSettings)
        val ibtnBookAppointments: ImageButton = view.findViewById(R.id.ibtnBookAppointment)
        val ibtnCalendar: ImageButton = view.findViewById(R.id.ibtnCalendar)
        val ibtnNotifications: ImageButton = view.findViewById(R.id.ibtnNotifications)
        val ibtnMaps: ImageButton = view.findViewById(R.id.ibtnMaps)
        val ibtnHealthzone: ImageButton = view.findViewById(R.id.ibtnHeathzone)
        val ibtnLogout: Button = view.findViewById(R.id.btnLogOut)

        notificationsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            notificationsList
        )

        // Set click listeners for navigation
        ibtnLogout.setOnClickListener {
            navigateTo(R.id.action_nav_menu_client_to_nav_login_client)
        }
        ibtnBookAppointments.setOnClickListener {
            navigateTo(R.id.action_nav_menu_client_to_nav_book_app_client1)
        }
        ibtnSettings.setOnClickListener {

            if (isOnline()) {
                navigateTo(R.id.action_nav_menu_client_to_nav_settings_client)
            } else {
                showToast("No Internet Connection!")
            }
        }
        ibtnCalendar.setOnClickListener {
            navigateTo(R.id.action_nav_menu_client_to_nav_calendar_client)
        }
        ibtnNotifications.setOnClickListener {

            if (isOnline()) {
                navigateTo(R.id.action_nav_menu_client_to_nav_notifications_client)
            } else {
                showToast("No Internet Connection!")
            }
        }
        ibtnMaps.setOnClickListener {

            if (isOnline()) {
                navigateTo(R.id.action_nav_menu_client_to_nav_maps_client)
            } else {
                showToast("No Internet Connection!")
            }
        }
        ibtnHealthzone.setOnClickListener {

            if (isOnline()) {
                navigateTo(R.id.action_nav_menu_client_to_nav_healthzone)
            } else {
                showToast("No Internet Connection!")
            }
        }
    }

    private fun navigateTo(actionId: Int) {
        findNavController().navigate(actionId)
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                Log.d("FCM", "Retrieved FCM Token: $fcmToken")
                fetchNotifications()
            } else {
                Log.e("FCM", "Failed to retrieve FCM Token")
            }
        }
    }

    private fun fetchNotifications() {
        // Ensure the fragment is currently attached to an activity
        if (!isAdded) return

        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("jwt_token", null)
        val authToken = token?.let { "$it" }
        val Id = loggedInClientUserId

        Log.d("NotificationsClient", "Attempting to fetch notifications...")
        Log.d("NotificationsClient", "AuthToken: $authToken, UserId: $Id, FCM Token: $fcmToken")

        // Check for null values
        if (Id == null || authToken == null || fcmToken == null) {
            Log.e("NotificationsClient", "Cannot fetch notifications: Missing UserId, AuthToken, or FCM Token")
            return
        }

        apiService.getPatientNotifications(authToken, Id, fcmToken!!).enqueue(object : Callback<NotificationsResponse> {
            override fun onResponse(call: Call<NotificationsResponse>, response: Response<NotificationsResponse>) {
                // Check if the fragment is still attached before updating the UI
                if (!isAdded) return

                Log.d("NotificationsClient", "API Response Code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val notifications = response.body()!!.notifications
                    Log.d("NotificationsClient", "Notifications fetched: ${notifications.size}")
                    notificationsList.clear()
                    notificationsList.addAll(notifications.map { it.message })
                    notificationsAdapter.notifyDataSetChanged()
                    updateNotificationCount(notifications.size)
                } else {
                    Log.e("NotificationsClient", "Failed to fetch notifications: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {
                // Check if the fragment is still attached before logging errors
                if (!isAdded) return
                Log.e("NotificationsClient", "API call failed: ${t.message}")
            }
        })
    }
    private fun updateNotificationCount(count: Int) {
        if (!isAdded) return
        txtNotificationCount.text = count.toString()
        txtNotificationCount.visibility = if (count > 0) View.VISIBLE else View.GONE
    }


    // Check network connectivity
    private fun isOnline(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

