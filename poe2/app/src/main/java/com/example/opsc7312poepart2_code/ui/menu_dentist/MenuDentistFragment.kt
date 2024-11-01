package com.example.poe2.ui.menu_dentist

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import android.util.Log


class MenuDentistFragment : Fragment() {
    private var dentistId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_menu_dentist, container, false)

        // Get the arguments passed from LoginDentistFragment
        arguments?.let { args ->
            dentistId = args.getString("dentistId")
            if (dentistId != null) {
                Log.d("MenuDentistFragment", "Received dentistId: $dentistId")
            } else {
                Log.d("MenuDentistFragment", "dentistId is null")
            }
        }

        // Initialize the ImageButtons
        val ibtnBookTimeOff: ImageButton = view.findViewById(R.id.ibtnBookTimeOff)
        val ibtnNotifications: ImageButton = view.findViewById(R.id.ibtnNotifications)
        val ibtnSettings: ImageButton = view.findViewById(R.id.ibtnSettings)
        val ibtnLogout: Button = view.findViewById(R.id.btnLogOut)


        ibtnLogout.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_login_dentist)
        }

        // Set OnClickListener for the Book Appointment button
        ibtnBookTimeOff.setOnClickListener {
            // Pass dentistId to BookTimeOffDentistFragment
            val bundle = Bundle().apply {
                putString("dentistId", dentistId)
            }
            findNavController().navigate(
                R.id.action_nav_menu_dentist_to_nav_book_time_off,
                bundle
            )
        }

        // Set OnClickListener for the Notifications button
        ibtnNotifications.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            //     findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_notifications_dentist)
            Toast.makeText(requireContext(), "TO BE IMPLEMENTED", Toast.LENGTH_SHORT).show()

        }

        // Set OnClickListener for the Notifications button
        ibtnSettings.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_settings_dentist)
        }

        return view // Make sure to return the view after setting up everything
    }
}