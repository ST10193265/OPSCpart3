package com.example.poe2.ui.healthzone

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.navigation.fragment.findNavController

import com.example.poe2.R

class HealthzoneFragment : Fragment() {


    private lateinit var wvFloss: WebView
    private lateinit var wvDentalTrouble: WebView
    private lateinit var wvXrays: WebView
    private lateinit var wvFillings: WebView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_healthzone, container, false)

        // Initialize the ImageButtons
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)

        // Set OnClickListener for the Book Appointment button
        ibtnHome.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_healthzone_to_nav_menu_client)
        }


        wvFloss = view.findViewById(R.id.wvFloss)
        wvDentalTrouble = view.findViewById(R.id.wvDentalTrouble)
        wvXrays = view.findViewById(R.id.wvXrays)
        wvFillings = view.findViewById(R.id.wvFillings)

        // Enable JavaScript
        wvFloss.settings.javaScriptEnabled = true
        wvDentalTrouble.settings.javaScriptEnabled = true
        wvXrays.settings.javaScriptEnabled = true
        wvFillings.settings.javaScriptEnabled = true

        // Set WebViewClients
        wvFloss.webViewClient = WebViewClient()
        wvDentalTrouble.webViewClient = WebViewClient()
        wvXrays.webViewClient = WebViewClient()
        wvFillings.webViewClient = WebViewClient()

        // Load YouTube video URLs
        wvFloss.loadUrl("https://www.youtube.com/watch?v=P6ZyFjwRtG0") // Example Video 1
        wvDentalTrouble.loadUrl("https://www.youtube.com/watch?v=MKGHCiULark") // Example Video 2
        wvXrays.loadUrl("https://www.youtube.com/watch?v=rbY8b0R5tkc") // Example Video 3
        wvFillings.loadUrl("https://www.youtube.com/watch?v=REUEbrpP6NM") // Example Video 4
        return view // Make sure to return the view after setting up everything
    }
}