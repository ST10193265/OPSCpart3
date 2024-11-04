package com.example.poe2.ui.client_settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.example.poe2.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class ClientSettingsFragment : Fragment() {

    // Declare UI elements
    private lateinit var spinnerLanguage: Spinner
    private lateinit var spinnerDistanceUnits: Spinner
    private lateinit var spinnerDistanceRadius: Spinner
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var ibtnHome: ImageButton
    private lateinit var database: DatabaseReference

    // Getters for the UI elements
    fun getSpinnerLanguage(): Spinner {
        return spinnerLanguage
    }

    fun getSpinnerDistanceUnits(): Spinner {
        return spinnerDistanceUnits
    }

    fun getSpinnerDistanceRadius(): Spinner {
        return spinnerDistanceRadius
    }

    fun getEtEmail(): EditText {
        return etEmail
    }

    fun getEtPhone(): EditText {
        return etPhone
    }

    // Inflate the fragment layout and initialize UI elements
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_client_settings, container, false)

        // Initialize Firebase database reference
        // Adapted from: Firebase Realtime Database Documentation
        // Source URL: https://firebase.google.com/docs/database/android/start
        // Contributors: Firebase Developers
        // Contributor Profile: https://firebase.google.com/profile/u/0/FirebaseDevelopers
        database = FirebaseDatabase.getInstance("https://opsc7312database-default-rtdb.firebaseio.com/").reference

        // Initialize UI elements from layout
        ibtnHome = view.findViewById(R.id.ibtnHome)
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage)
        spinnerDistanceUnits = view.findViewById(R.id.spinnerDistanceUnits)
        spinnerDistanceRadius = view.findViewById(R.id.spinnerDistanceRadius)
        etEmail = view.findViewById(R.id.etEmail)
        etPhone = view.findViewById(R.id.etPhone)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

        // Set up language spinner with predefined languages
        // Adapted from: Android Spinner Documentation
        // Source URL: https://developer.android.com/guide/topics/ui/controls/spinner
        // Contributors: Android Developers
        // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
        val languages = arrayOf("English", "Afrikaans")
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        spinnerLanguage.adapter = languageAdapter

        // Set up distance units spinner with options
        // Adapted from: Android Spinner Documentation
        // Source URL: https://developer.android.com/guide/topics/ui/controls/spinner
        // Contributors: Android Developers
        // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
        val distanceUnits = arrayOf("km", "m")
        val distanceUnitsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, distanceUnits)
        spinnerDistanceUnits.adapter = distanceUnitsAdapter
        spinnerDistanceUnits.setSelection(0) // Default to kilometers

        // Set up distance radius spinner with options
        // Adapted from: Android Spinner Documentation
        // Source URL: https://developer.android.com/guide/topics/ui/controls/spinner
        // Contributors: Android Developers
        // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
        val distanceRadius = arrayOf("No Limit", "1", "5", "10", "20", "30")

        val distanceRadiusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, distanceRadius)
        spinnerDistanceRadius.adapter = distanceRadiusAdapter
        spinnerDistanceRadius.setSelection(0)

        // Load existing settings from Firebase
        loadSettings()

        // Set up click listeners
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
        }

        btnSave.setOnClickListener {
            saveSettings() // Save settings when Save button is clicked
        }

        btnCancel.setOnClickListener {
            clearFields() // Clear input fields when Cancel button is clicked
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)// Navigate back to home after clearing fields
        }

        return view
    }
    // Save the settings to Firebase
    // Adapted from: Firebase Realtime Database Documentation
    // Source URL: https://firebase.google.com/docs/database/android/start
    // Contributors: Firebase Developers
    // Contributor Profile: https://firebase.google.com/profile/u/0/FirebaseDevelopers
    private fun saveSettings() {
        // Get selected values from UI elements
        val selectedLanguage = spinnerLanguage.selectedItem.toString()
        val selectedDistanceUnit = spinnerDistanceUnits.selectedItem.toString()
        val selectedDistanceRadius = spinnerDistanceRadius.selectedItem.toString()
        val email = etEmail.text.toString().trim()
        val phoneNumber = etPhone.text.toString().trim()

        // Create a map to hold the updated settings
        val updatedData = mutableMapOf<String, Any>()

        // Map language to its code for Firebase storage
        updatedData["language"] = if (selectedLanguage == "Afrikaans") "af" else "en"

        // Store distance unit and radius in the map
        updatedData["distanceUnit"] = selectedDistanceUnit
        updatedData["distanceRadius"] = selectedDistanceRadius

        // Only store email and phone number if they are not empty
        if (email.isNotEmpty()) {
            updatedData["email"] = email
        }
        if (phoneNumber.isNotEmpty()) {
            updatedData["phoneNumber"] = phoneNumber
        }

        // Get the client ID for Firebase
        val clientId = loggedInClientUserId

        // If there are updates to be made, call updateSettings()
        if (updatedData.isNotEmpty()) {
            if (clientId != null) {
                updateSettings(clientId, updatedData)
            }
        } else {
            // Show a message if no changes were made
            Toast.makeText(requireContext(), "No changes made!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
        }
    }

    // Update the settings in Firebase
    // Adapted from: Firebase Realtime Database Documentation
    // Source URL: https://firebase.google.com/docs/database/android/start
    // Contributors: Firebase Developers
    // Contributor Profile: https://firebase.google.com/profile/u/0/FirebaseDevelopers
    private fun updateSettings(clientId: String, updatedData: Map<String, Any>) {
        // Update the client's data in Firebase
        val selectedLanguage = spinnerLanguage.selectedItem.toString()
        database.child("clients/$clientId").updateChildren(updatedData)
            .addOnSuccessListener {
                // Show success message and update app language if language was changed
                Toast.makeText(context, "Settings updated successfully!", Toast.LENGTH_SHORT).show()
                updateAppLanguage(selectedLanguage)
                findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
            }
            .addOnFailureListener { exception ->
                // Show an error message if the update failed
                Toast.makeText(context, "Error updating settings: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Update the app's language and restart the activity to apply the changes
    // Adapted from: Android Localization Documentation
    // Source URL: https://developer.android.com/guide/topics/resources/localization
    // Contributors: Android Developers
    // Contributor Profile: https://developer.android.com/profile/u/0/AndroidDevelopers
    private fun updateAppLanguage(language: String) {
        val locale = if (language == "Afrikaans") Locale("af") else Locale("en")
        Locale.setDefault(locale)

        // Update the app's configuration with the new locale
        val config = requireContext().resources.configuration
        config.setLocale(locale)

        // Apply the updated configuration
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)

        // Recreate the activity to apply the language change
        requireActivity().recreate()
    }

    // Load existing settings from Firebase
    // Adapted from: Firebase Realtime Database Documentation
    // Source URL: https://firebase.google.com/docs/database/android/start
    // Contributors: Firebase Developers
    // Contributor Profile: https://firebase.google.com/profile/u/0/FirebaseDevelopers
    fun loadSettings() {
        val clientId = loggedInClientUserId
        // Retrieve settings from Firebase for the logged-in client
        database.child("clients/$clientId").get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    // Extract and set values to UI elements
                    val language = dataSnapshot.child("language").value as? String ?: "en"
                    val distanceUnit = dataSnapshot.child("distanceUnit").value as? String ?: "km"
                    val distanceRadius = dataSnapshot.child("distanceRadius").value as? String ?: "No Limit"
                    val email = dataSnapshot.child("email").value as? String ?: ""
                    val phoneNumber = dataSnapshot.child("phoneNumber").value as? String ?: ""

                    // Set spinner and EditText fields with the retrieved values
                    spinnerLanguage.setSelection(if (language == "af") 1 else 0)
                    spinnerDistanceUnits.setSelection(if (distanceUnit == "km") 0 else 1)
                    val distanceOptions = arrayOf("No Limit", "1", "5", "10", "20", "30")
                    val distanceRadiusIndex = distanceOptions.indexOf(distanceRadius)
                    spinnerDistanceRadius.setSelection(if (distanceRadiusIndex != -1) distanceRadiusIndex else 0)

                    etEmail.setText(email)
                    etPhone.setText(phoneNumber)
                }
            }
            .addOnFailureListener {
                // Show an error message if settings could not be loaded
                Toast.makeText(requireContext(), "Failed to load settings", Toast.LENGTH_SHORT).show()
            }
    }


    // Clear all input fields and reset spinners to their default values
    private fun clearFields() {
        spinnerLanguage.setSelection(0)
        spinnerDistanceUnits.setSelection(0)
        spinnerDistanceRadius.setSelection(0)
        etEmail.text.clear()
        etPhone.text.clear()
    }


}
