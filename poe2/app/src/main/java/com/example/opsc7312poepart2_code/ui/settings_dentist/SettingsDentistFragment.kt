package com.example.poe2.ui.settings_dentist

import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment
import com.example.poe2.BuildConfig
import com.example.poe2.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class SettingsDentistFragment : Fragment() {

    // UI components for the dentist's settings screen
    private lateinit var spinnerLanguageD: Spinner // Spinner for selecting the language
    private lateinit var etAddress: AutoCompleteTextView // AutoCompleteTextView for address input with suggestions
    private lateinit var etPhoneD: EditText // EditText for inputting the phone number
    private lateinit var btnSaveD: Button // Button to save the settings
    private lateinit var btnCancelD: Button // Button to cancel the changes and navigate back
    private lateinit var ibtnHomeD: ImageButton // ImageButton to navigate back to the home screen

    // Firebase reference for database interaction
    private lateinit var database: DatabaseReference
    private lateinit var placesClient: PlacesClient // Google Places API client for place suggestions
    private var destinationLatLng: LatLng? = null // Variable to store the selected destination's LatLng
    private val apiKey = BuildConfig.MAPS_API_KEY // Google Maps API key
    private var placeSuggestions: List<String> = emptyList() // List to store place suggestions
    private var isAddressValid = false // Boolean to track if the selected address is valid

    // Getter methods for the UI components
    fun getSpinnerLanguageD(): Spinner {
        return spinnerLanguageD
    }

    fun getEtAddress(): AutoCompleteTextView {
        return etAddress
    }

    fun getEtPhoneD(): EditText {
        return etPhoneD
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings_dentist, container, false)

        // Initialize Firebase database reference
        // Adapted from: Firebase Realtime Database Documentation
        // Source URL: https://firebase.google.com/docs/database/android/start
        // Contributors: Firebase Developers
        // Contributor Profile: https://firebase.google.com/profile/u/0/FirebaseDevelopers
        database = FirebaseDatabase.getInstance("https://opsc7312database-default-rtdb.firebaseio.com/").reference

        // Initialize the Google Places API
        // Adapted from: Google Places API Documentation
        // Source URL: https://developers.google.com/places/android-sdk/start
        // Contributors: Google Developers
        // Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
        Places.initialize(requireContext(), apiKey)
        placesClient = Places.createClient(requireContext())

        // Initialize the UI components
        initializeUIComponents(view)

        // Load the saved language preference from the database
        loadLanguagePreference()

        // Set click listener to navigate to the home screen when the home button is clicked
        ibtnHomeD.setOnClickListener {
            navigateToHome()
        }

        // Set click listener to save the settings when the save button is clicked
        btnSaveD.setOnClickListener {
            saveSettings()
        }

        // Set click listener to clear fields and navigate back when the cancel button is clicked
        btnCancelD.setOnClickListener {
            clearFields()
            navigateToHome()
        }

        // Set up the AutoCompleteTextView for address suggestions
        setupAutoCompleteForAddress()

        return view
    }

    // Method to initialize UI components and set up the language spinner
    private fun initializeUIComponents(view: View) {
        ibtnHomeD = view.findViewById(R.id.ibtnHomeD)
        spinnerLanguageD = view.findViewById(R.id.spinnerLanguageD)
        etAddress = view.findViewById(R.id.etAddress)
        etPhoneD = view.findViewById(R.id.etPhoneD)
        btnSaveD = view.findViewById(R.id.btnSaveD)
        btnCancelD = view.findViewById(R.id.btnCancelD)

        // Setting up the language options in the spinner
        val languages = arrayOf("English", "Afrikaans")
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        spinnerLanguageD.adapter = languageAdapter
    }
    // Method to save the dentist's settings
    // Adapted from: Android Spinner Documentation
    // Source URL: https://developer.android.com/guide/topics/ui/controls/spinner
    // Contributors: Android Developers
    // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
    private fun saveSettings() {
        // Get the selected language from the spinner
        val selectedLanguage = spinnerLanguageD.selectedItem.toString()
        val updatedData = mutableMapOf<String, Any>()

        // Add the selected language to the update map
        if (selectedLanguage.isNotEmpty()) {
            updatedData["language"] = selectedLanguage
        }

        // Get the entered address, ensure it's valid before saving
        val address = etAddress.text.toString().trim()
        if (address.isNotEmpty()) {
            if (!isAddressValid) {
                Toast.makeText(requireContext(), "Please select a valid address from suggestions.", Toast.LENGTH_SHORT).show()
                return
            } else {
                updatedData["address"] = address
            }
        }

        // Get the entered phone number
        val phoneNumber = etPhoneD.text.toString().trim()
        if (phoneNumber.isNotEmpty()) {
            updatedData["phoneNumber"] = phoneNumber
        }

        // Get the dentist's ID from the logged-in user
        val dentistId = LoginDentistFragment.loggedInDentistUserId

        // If the dentist ID and update data are available, update the settings in the database
        if (dentistId != null && updatedData.isNotEmpty()) {
            updateSettings(dentistId, updatedData)
        } else {
            Toast.makeText(requireContext(), "No changes made!", Toast.LENGTH_SHORT).show()
            navigateToHome()
        }
    }

    // Method to update the settings in Firebase and change the app's language
    // Adapted from: Firebase Realtime Database Documentation
    // Source URL: https://firebase.google.com/docs/database/android/start
    // Contributors: Firebase Developers
    // Contributor Profile: https://firebase.google.com/profile/u/0/FirebaseDevelopers
    private fun updateSettings(dentistId: String, updatedData: Map<String, Any>) {
        val selectedLanguage = spinnerLanguageD.selectedItem.toString()
        database.child("dentists/$dentistId").updateChildren(updatedData)
            .addOnSuccessListener {
                Toast.makeText(context, "Settings updated successfully!", Toast.LENGTH_SHORT).show()
                updateAppLanguage(selectedLanguage)
                navigateToHome()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error updating settings: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Method to update the app's language based on the dentist's preference
    // Adapted from: Android Localization Documentation
    // Source URL: https://developer.android.com/guide/topics/resources/localization
    // Contributors: Android Developers
    // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
    private fun updateAppLanguage(language: String) {
        val locale = if (language == "Afrikaans") Locale("af") else Locale("en")
        Locale.setDefault(locale)

        val config = requireContext().resources.configuration
        config.setLocale(locale)

        // Update the configuration and recreate the activity to apply changes
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)

        requireActivity().recreate() // Refresh the activity to apply the new language
    }

    // Method to navigate back to the home screen
    // Adapted from: Android Navigation Component Documentation
    // Source URL: https://developer.android.com/guide/navigation
    // Contributors: Android Developers
    // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
    private fun navigateToHome() {
        findNavController().navigate(R.id.action_nav_settings_dentist_to_nav_menu_dentist)
    }
    // Set up AutoCompleteTextView for address suggestions using Google Places API
    // Adapted from: Google Places API Documentation
    // Source URL: https://developers.google.com/places/android-sdk/autocomplete
    // Contributors: Google Developers
    // Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
    private fun setupAutoCompleteForAddress() {
        val autocompleteAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, emptyList<String>())
        etAddress.setAdapter(autocompleteAdapter)

        // Handle item clicks from address suggestions
        etAddress.setOnItemClickListener { parent, _, position, _ ->
            val selectedPlace = parent.getItemAtPosition(position) as String
            isAddressValid = true
            findPlace(selectedPlace)
        }

        // Add a TextWatcher to listen for text changes and fetch suggestions
        etAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    fetchPlaceSuggestions(s.toString())
                    isAddressValid = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // Fetch place suggestions from Google Places API based on the user's query
// Adapted from: Google Places API Documentation
// Source URL: https://developers.google.com/places/android-sdk/autocomplete
// Contributors: Google Developers
// Contributor Profile: https://developers.google.com/profile/u/0/GoogleDevelopers
    private fun fetchPlaceSuggestions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            placeSuggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, placeSuggestions)
            etAddress.setAdapter(adapter)
            adapter.notifyDataSetChanged()
        }
    }

    // Find the place using the selected place name and update the destinationLatLng
// Adapted from: Android Geocoder Documentation
// Source URL: https://developer.android.com/reference/android/location/Geocoder
// Contributors: Android Developers
// Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
    private fun findPlace(placeName: String) {
        val geocoder = Geocoder(requireContext())
        val addressList = geocoder.getFromLocationName(placeName, 1)
        if (addressList != null && addressList.isNotEmpty()) {
            val address = addressList[0]
            destinationLatLng = LatLng(address.latitude, address.longitude)
        }
    }

    // Load the saved language preference from the Firebase database
// Adapted from: Firebase Realtime Database Documentation
// Source URL: https://firebase.google.com/docs/database/android/start
// Contributors: Firebase Developers
// Contributor Profile: https://firebase.google.com/profile/u/0/FirebaseDevelopers
    fun loadLanguagePreference() {
        database.child("Dentists/settings/language").get()
            .addOnSuccessListener { dataSnapshot ->
                val language = dataSnapshot.value as? String ?: "en"
                spinnerLanguageD.setSelection(if (language == "af") 1 else 0)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load settings", Toast.LENGTH_SHORT)
                    .show()
            }
    }


    // Clear the input fields in the settings form
    private fun clearFields() {
        spinnerLanguageD.setSelection(0)
        etAddress.setText("")
        etPhoneD.setText("")
    }
}




