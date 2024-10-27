package com.example.poe2.ui.menu_client

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.loggedInDentistUserId
import com.example.poe2.R

class MenuClientFragment : Fragment() {



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_menu_client, container, false)

        //Initialize the ImageButtons
        val ibtnSettings: ImageButton = view.findViewById(R.id.ibtnSettings)
        val ibtnBookAppointments: ImageButton = view.findViewById(R.id.ibtnBookAppointment)
        val ibtnCalendar: ImageButton = view.findViewById(R.id.ibtnCalendar)
        val ibtnNotifications: ImageButton = view.findViewById(R.id.ibtnNotifications)
        val ibtnMaps: ImageButton = view.findViewById(R.id.ibtnMaps)
        val ibtnHealthzone: ImageButton = view.findViewById(R.id.ibtnHeathzone)
        val ibtnLogout: Button = view.findViewById(R.id.btnLogOut)

        ibtnLogout.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_client_to_nav_login_client)
        }

        // Set OnClickListener for Book Appointment button
        ibtnBookAppointments.setOnClickListener {
            // Navigate to the BookAppointmentClientFragment1 using the NavController
            findNavController().navigate(R.id.action_nav_menu_client_to_nav_book_app_client1)
            Log.e("Book App Client 1", "button clicked")
        }

        // Set OnClickListener for Settings button
        ibtnSettings.setOnClickListener {
            // Navigate to the ClientSettingsFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_client_to_nav_settings_client)
        }



        // Set OnClickListener for Calendar button
        ibtnCalendar.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_client_to_nav_calendar_client)
        }

        // Set OnClickListener for Notification button
        ibtnNotifications.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_client_to_nav_notifications_client)


        }

        // Set OnClickListener for Maps button
        ibtnMaps.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_client_to_nav_maps_client)
        }

        // Set OnClickListener for Healthzone button
        ibtnHealthzone.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_client_to_nav_healthzone)
        }

        return view
    }
}
