package com.example.poe2.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.poe2.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize the binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup the click listener for the Register Client button
        binding.btnRegisterClient.setOnClickListener {
            // Navigate to the Client Register fragment
            findNavController().navigate(R.id.action_nav_home_to_nav_register_client)
        }

        binding.btnRegisterDentist.setOnClickListener {
            // Navigate to the Client Register fragment
            findNavController().navigate(R.id.action_nav_home_to_nav_register_dentist)
        }

        binding.btnLoginClient.setOnClickListener {
            // Navigate to the Client Register fragment
            Log.d("HomeFragment", "Login Client Button Clicked")
            findNavController().navigate(R.id.action_nav_home_to_nav_login_client)
        }

        binding.btnLoginDentist.setOnClickListener{
            Log.d("HomeFragment", "Login Dentist Button Clicked")
            findNavController().navigate(R.id.action_nav_home_to_nav_login_dentist)
        }

        // Return the root view
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding reference
        _binding = null
    }
}
