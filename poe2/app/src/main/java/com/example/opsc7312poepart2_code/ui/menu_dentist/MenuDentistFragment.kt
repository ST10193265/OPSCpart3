package com.example.poe2.ui.menu_dentist

import android.content.Context
import android.net.ConnectivityManager
import androidx.fragment.app.viewModels
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
import com.example.poe2.R

class MenuDentistFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_menu_dentist, container, false)

        // Initialize the ImageButtons
        val ibtnViewApp:  ImageButton = view.findViewById(R.id.ibtnViewApp)

        val ibtnSettings: ImageButton = view.findViewById(R.id.ibtnSettings)
        val ibtnLogout: Button = view.findViewById(R.id.btnLogOut)
        val ibtnMagageApp: ImageButton = view.findViewById(R.id.ibtnMagageApp)
        val ibtnTimeOff: ImageButton = view.findViewById(R.id.ibtnBooktimeOff)

        ibtnMagageApp.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController

            if (isOnline()) {
                findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_dentist_app)
            } else {
                showToast("No Internet Connection!")
            }
        }

        ibtnTimeOff.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_dentist_to_bookTimeOffFragment)

        }


        ibtnLogout.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_login_dentist)

        }

        // Set OnClickListener for the Book Appointment button
        ibtnViewApp.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_view_appointment_dentist)

        }



        // Set OnClickListener for the Notifications button
        ibtnSettings.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController

            if (isOnline()) {
                findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_settings_dentist)
            } else {
                showToast("No Internet Connection!")
            }
        }

        return view // Make sure to return the view after setting up everything
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
