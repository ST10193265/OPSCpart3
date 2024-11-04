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
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.findNavController
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.NotificationsResponse
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.loggedInDentistUserId
import com.example.poe2.R
import com.example.poe2.databinding.FragmentNotificationsDentistBinding
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsDentistFragment : Fragment() {


    private var _binding: FragmentNotificationsDentistBinding? = null
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
        _binding = FragmentNotificationsDentistBinding.inflate(inflater, container, false)
        val view = inflater.inflate(R.layout.fragment_notifications_dentist, container, false)

        // Initialize ApiService with context
        apiService = ApiClient.createApiService(requireContext())

        // Initialize UI elements from layout
        ibtnHome = view.findViewById(R.id.ibtnHome)

        // Initialize the ListView adapter
        notificationsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            notificationsList
        )
        binding.appointmentsListView.adapter = notificationsAdapter

        // Fetch FCM token in the background
        fetchFcmToken()

        // Set up click listeners
        ibtnHome.setOnClickListener {
            navigateToHome()
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Register the broadcast receiver for real-time updates
        requireContext().registerReceiver(
            notificationReceiver,
            IntentFilter("FCM_NOTIFICATION"),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    // Fetch the FCM token for the dentist
    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                fetchNotifications() // Fetch notifications once token is available
            } else {
                Log.e("FCM", "Failed to retrieve FCM Token")
            }
        }
    }

    // Fetch notifications from the server
    private fun fetchNotifications() {
        val dentistId = loggedInDentistUserId
        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("jwt_token", null)
        val authToken = token?.let { "$it" }

        if (dentistId != null && fcmToken != null) {
            if (authToken != null) {
                apiService.getDentistNotifications(authToken, dentistId, fcmToken!!).enqueue(object : Callback<NotificationsResponse> {
                    override fun onResponse(call: Call<NotificationsResponse>, response: Response<NotificationsResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val notifications = response.body()!!.notifications
                            notificationsList.clear()
                            notificationsList.addAll(notifications.map { it.message })
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
        } else {
            Log.e("NotificationsDentist", "Cannot fetch notifications: Missing DentistId or FCM Token")
        }
    }

    // Broadcast receiver for handling notifications
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

    // Method to get the logged-in dentist's ID
    private fun getLoggedInDentistId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_nav_notifications_dentist_to_nav_menu_dentist)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireContext().unregisterReceiver(notificationReceiver)
    }
}


