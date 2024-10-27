package com.example.poe2.ui.maps_client

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.BuildConfig
import com.example.poe2.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
class MapsClientFragment : Fragment(), OnMapReadyCallback {

    // Variables for location services, map, and UI components
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var spinnerDentists: Spinner
    private lateinit var btnGoNow: Button
    private lateinit var map: GoogleMap
    private var destinationLatLng: LatLng? = null
    private val apiKey = BuildConfig.MAPS_API_KEY
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var textViewDirection: TextView
    private lateinit var tts: TextToSpeech // Text-to-Speech engine
    private var currentStep: Int = 0 // Keeps track of current direction step
    private val steps = mutableListOf<String>() // Stores direction steps

    // Getter methods for UI components
    fun getSpinnerDentists(): Spinner {
        return spinnerDentists
    }

    fun getBtnGoNow(): Button {
        return btnGoNow
    }

    fun getTextViewDirection(): TextView {
        return textViewDirection
    }

    // Getter method for destination LatLng
    fun getDestinationLatLng(latLng: LatLng): LatLng? {
        return destinationLatLng
    }

    // Inflates the view and initializes necessary components
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_maps_client, container, false)

        // Initialize fused location services to get the user's location
        // Adapted from: Google Maps API Documentation
        // Source URL: https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial
        // Contributors: Google Developers
        // Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize Places SDK with the API key
        // Adapted from: Google Places API Documentation
        // Source URL: https://developers.google.com/places/android-sdk/start
        // Contributors: Google Developers
        // Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
        Places.initialize(requireContext(), apiKey)

        // Initialize UI components
        spinnerDentists = view.findViewById(R.id.spinnerDentists)
        btnGoNow = view.findViewById(R.id.btnGoNow)
        textViewDirection = view.findViewById(R.id.textViewDirection)

        // Initialize Text-to-Speech
        // Adapted from: Android Text-to-Speech Documentation
        // Source URL: https://developer.android.com/reference/android/speech/tts/TextToSpeech
        // Contributors: Android Developers
        // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        // Set up map fragment
        // Adapted from: Google Maps Fragment Documentation
        // Source URL: https://developers.google.com/maps/documentation/android-sdk/map
        // Contributors: Google Developers
        // Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Home button click listener to navigate back
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_maps_client_to_nav_menu_client)
        }

        // Fetch dentist data to populate the spinner
        fetchDentistData()

        // Handle "Go Now" button click to fetch directions
        btnGoNow.setOnClickListener {
            if (destinationLatLng != null) {
                getDirections(destinationLatLng!!)
                Toast.makeText(requireContext(), "Fetching directions...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please select a destination", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // Fetches dentist data from Firebase and populates the spinner
    // Adapted from: Firebase Realtime Database Documentation
    // Source URL: https://firebase.google.com/docs/database/android/start
    // Contributors: Firebase Developers
    // Contributor Profile: https://firebase.google.com/profile/u/0/FirebaseDevelopers
    fun fetchDentistData() {
        val dentistList = mutableListOf<String>()
        val addressMap = mutableMapOf<String, String>()
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("dentists")

        // Retrieve dentist information from Firebase
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dentistSnapshot in dataSnapshot.children) {
                    val name = dentistSnapshot.child("name").getValue(String::class.java) ?: continue
                    val address = dentistSnapshot.child("address").getValue(String::class.java) ?: continue

                    dentistList.add(name)
                    addressMap[name] = address
                }

                // Set up spinner adapter and item selection listener
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, dentistList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDentists.adapter = adapter

                spinnerDentists.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                        val selectedDentist = dentistList[position]
                        val selectedAddress = addressMap[selectedDentist]
                        selectedAddress?.let { findPlace(it) }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load dentist data: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // Finds a location by address and places a marker on the map
    // Adapted from: Android Geocoder Documentation
    // Source URL: https://developer.android.com/reference/android/location/Geocoder
    // Contributors: Android Developers
    // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
    fun findPlace(addressString: String) {
        val geocoder = Geocoder(requireContext())
        try {
            val addressList = geocoder.getFromLocationName(addressString, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                destinationLatLng = LatLng(address.latitude, address.longitude)

                // Add a marker for the destination
                map.addMarker(MarkerOptions().position(destinationLatLng!!).title("Destination: $addressString"))

                // Zoom into the selected location
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng!!, 22f))
            } else {
                Toast.makeText(requireContext(), "Address not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Geocoder service is not available", Toast.LENGTH_SHORT).show()
        }
    }

    // Requests directions from the current location to the destination
    // Adapted from: Google Maps Directions API Documentation
    // Source URL: https://developers.google.com/maps/documentation/directions/start
    // Contributors: Google Developers
    // Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
    fun getDirections(destination: LatLng) {
        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
            return
        }

        // Retrieve the user's last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val originLatLng = LatLng(location.latitude, location.longitude)
                val url = buildDirectionsUrl(originLatLng, destination)
                fetchDirections(url)
            } else {
                Log.d("MapsClientFragment", "Unable to get current location")
            }
        }.addOnFailureListener {
            Log.d("MapsClientFragment", "Failed to retrieve location.")
            Toast.makeText(requireContext(), "Failed to retrieve location", Toast.LENGTH_SHORT).show()
        }
    }

    // Requests location permissions from the user
    // Adapted from: Android Location Permissions Documentation
    // Source URL: https://developer.android.com/training/permissions/requesting
    // Contributors: Android Developers
    // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
    fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    // Builds the URL to fetch directions from Google Directions API
    // Adapted from: Google Maps Directions API Documentation
    // Source URL: https://developers.google.com/maps/documentation/directions/start
    // Contributors: Google Developers
    // Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
    fun buildDirectionsUrl(origin: LatLng, destination: LatLng): String {
        val str_origin = "origin=${origin.latitude},${origin.longitude}"
        val str_dest = "destination=${destination.latitude},${destination.longitude}"
        val sensor = "sensor=false"
        return "https://maps.googleapis.com/maps/api/directions/json?$str_origin&$str_dest&$sensor&key=$apiKey"
    }

    // Fetches directions data from the API
    // Adapted from: OkHttp Documentation
    // Source URL: https://square.github.io/okhttp/
    // Contributors: Square, Inc.
    // Contributor Profile: https://github.com/square/okhttp
    fun fetchDirections(url: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to fetch directions: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val jsonData = responseBody.string()
                        requireActivity().runOnUiThread {
                            parseDirectionsJson(jsonData)
                        }
                    } ?: run {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Response body is null", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Response not successful: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Parses the JSON response from Google Directions API to extract route information
    // Adapted from: Google Maps Directions API Documentation
    // Source URL: https://developers.google.com/maps/documentation/directions/start
    // Contributors: Google Developers
    // Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
    fun parseDirectionsJson(jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            val routes = jsonObject.getJSONArray("routes")

            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val legs = route.getJSONArray("legs")
                steps.clear()

                // Extract and display estimated travel time
                val duration = legs.getJSONObject(0).getJSONObject("duration")
                val durationText = duration.getString("text")
                textViewDirection.text = "Estimated travel time: $durationText\n"

                // Extract individual steps and directions
                for (i in 0 until legs.length()) {
                    val stepArray = legs.getJSONObject(i).getJSONArray("steps")
                    for (j in 0 until stepArray.length()) {
                        val step = stepArray.getJSONObject(j)
                        val instruction = step.getString("html_instructions").replace("<[^>]*>".toRegex(), "")
                        steps.add(instruction)
                    }
                }

                // Display and optionally speak the first direction step
                if (steps.isNotEmpty()) {
                    currentStep = 0
                    val firstStep = steps[currentStep]
                    textViewDirection.append("Direction 1: $firstStep\n")
                    tts.speak(firstStep, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                Toast.makeText(requireContext(), "No routes found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: JSONException) {
            Toast.makeText(requireContext(), "Error parsing JSON", Toast.LENGTH_SHORT).show()
        }
    }

    // Callback method when the map is ready
    // Adapted from: Google Maps API Documentation
    // Source URL: https://developers.google.com/maps/documentation/android-sdk/map
    // Contributors: Google Developers
    // Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        } else {
            requestLocationPermissions()
        }
    }
}
