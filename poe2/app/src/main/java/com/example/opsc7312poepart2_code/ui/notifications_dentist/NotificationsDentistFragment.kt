package com.example.poe2.ui.notifications_dentist

import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.loggedInDentistUserId
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

import com.example.poe2.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class NotificationsDentistFragment : Fragment() {

    private lateinit var database: DatabaseReference

    private val appointmentList = mutableListOf<String>()
    private lateinit var appointmentAdapter: ArrayAdapter<String>
    private lateinit var listView: ListView
    private val notificationPermissionRequestCode = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notifications_dentist, container, false)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("appointments")

        // Get the logged-in dentist ID
        loggedInDentistUserId = getLoggedInDentistId()

        // Initialize views
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)
        listView = view.findViewById(R.id.appointmentsListView)

        // Set up the ListView adapter
        appointmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, appointmentList)
        listView.adapter = appointmentAdapter

        // Set the home button listener to navigate to the main menu
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_notifications_dentist_to_nav_menu_dentist)
        }

        // Start listening for new appointments for this dentist
        if ( loggedInDentistUserId != null) {
            listenForNewAppointments( loggedInDentistUserId!!)
        } else {
            Toast.makeText(requireContext(), "Dentist not logged in.", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun listenForNewAppointments(dentistId: String) {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val appointmentDentistId = snapshot.child("dentistId").getValue(String::class.java)
                if (appointmentDentistId == dentistId) {
                    val appointmentDate = snapshot.child("date").getValue(String::class.java) ?: "N/A"
                    val appointmentSlot = snapshot.child("slot").getValue(String::class.java) ?: "N/A"
                    val appointmentDescription = snapshot.child("description").getValue(String::class.java) ?: "N/A"

                    val appointmentDetails = "Date: $appointmentDate, Slot: $appointmentSlot, Description: $appointmentDescription"

                    // Add the new appointment to the list and notify the adapter
                    appointmentList.add(appointmentDetails)
                    appointmentAdapter.notifyDataSetChanged()

                    alertDentistNewAppointment(appointmentDate, appointmentSlot, appointmentDescription)
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

    private fun alertDentistNewAppointment(date: String, slot: String, description: String) {
        val message = "New Appointment on $date at $slot: $description"
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        // Check if notification permission is granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // If permission is granted, send notification
            sendNotification(message)
        } else {
            // Request notification permission
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), notificationPermissionRequestCode)
        }
    }

    private fun sendNotification(message: String) {
        // Create the notification channel if required
        createNotificationChannel()

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(requireContext(), "APPOINTMENTS_CHANNEL_ID")
            .setSmallIcon(R.drawable.redcircle) // Replace with your actual app icon
            .setContentTitle("New Appointment")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Ensure that permission is checked before calling notify
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(requireContext())) {
                notify(1, notificationBuilder.build())
            }
        } else {
            // Log or handle the case where the permission is not available
            Toast.makeText(requireContext(), "Notification permission not granted.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Appointments"
            val descriptionText = "Notifications for new appointments"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("APPOINTMENTS_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getLoggedInDentistId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationPermissionRequestCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission was granted, re-send the notification
                sendNotification("New Appointment Available")
            } else {
                Toast.makeText(requireContext(), "Notification permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
