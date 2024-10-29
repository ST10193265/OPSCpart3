package com.example.opsc7312poepart2_code.ui.register_dentist

import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.BuildConfig
import com.example.poe2.R
import com.example.poe2.databinding.FragmentRegisterDentistBinding
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest
import java.security.SecureRandom

class RegisterDentistFragment : Fragment() {

    private var _binding: FragmentRegisterDentistBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference
    private lateinit var placesClient: PlacesClient
    private var destinationLatLng: LatLng? = null
    private val apiKey = BuildConfig.MAPS_API_KEY
    private var placeSuggestions: List<String> = emptyList()
    private var isAddressValid = false
    private var passwordVisible = false // Track password visibility state

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterDentistBinding.inflate(inflater, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("dentists") // Node for dentists

        // Initialize Places API for autocomplete
        Places.initialize(requireContext(), apiKey)
        placesClient = Places.createClient(requireContext())


        // Set password visibility to hidden by default
        binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Set up button click listeners
        binding.btnCancel.setOnClickListener {
            clearFields()
        }

        binding.btnRegister.setOnClickListener {
            onRegisterClick()
        }

        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility(it)
        }

        // Setup autocomplete for address
        setupAutoCompleteForAddress()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onRegisterClick() {
        val name = binding.etxtName.text.toString().trim()
        val address = binding.etxtAddress.text.toString().trim()
        val email = binding.etxtEmail.text.toString().trim()
        val username = binding.etxtUsername.text.toString().trim()
        val password = binding.etxtPassword.text.toString().trim()
        val phoneNumber = binding.etxtPhoneNumber.text.toString().trim()

        if (!isValidInput(name, address, email, username, password, phoneNumber)) {
            return
        }

        registerUser(name, address, email, username, password, phoneNumber)
    }

    private fun isValidInput(
        name: String, address: String, email: String, username: String, password: String, phoneNumber: String
    ): Boolean {
        if (name.isEmpty()) {
            showToast("Please enter your name.")
            return false
        }

        if (address.isEmpty()) {
            showToast("Please enter your address.")
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email address.")
            return false
        }

        if (username.isEmpty()) {
            showToast("Please enter a username.")
            return false
        }

        if (password.length < 6) {
            showToast("Password must be at least 6 characters long.")
            return false
        }

        if (phoneNumber.isEmpty()) {
            showToast("Please enter a valid phone number.")
            return false
        }

        return true
    }

    private fun registerUser(
        name: String, address: String, email: String, username: String, password: String, phoneNumber: String
    ) {
        val userId = dbReference.push().key ?: return showToast("Failed to generate user ID")

        val salt = generateSalt()
        val hashedPassword = hashPassword(password, salt)

        val user = hashMapOf(
            "userId" to userId,
            "name" to name,
            "address" to address,
            "email" to email,
            "username" to username,
            "password" to hashedPassword,
            "salt" to Base64.encodeToString(salt, Base64.DEFAULT),
            "phoneNumber" to phoneNumber,
            "isPasswordUpdated" to false
        )

        dbReference.child(userId).setValue(user)
            .addOnSuccessListener {
                showToast("Data saved successfully!")
                clearFields()
                findNavController().navigate(R.id.action_nav_register_dentist_to_nav_login_dentist)
            }
            .addOnFailureListener { exception ->
                showToast("Error saving data: ${exception.message}")
            }
    }

    private fun generateSalt(): ByteArray {
        return ByteArray(16).apply { SecureRandom().nextBytes(this) }
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
    }

    private fun clearFields() {
        with(binding) {
            etxtName.text.clear()
            etxtAddress.text.clear()
            etxtEmail.text.clear()
            etxtUsername.text.clear()
            etxtPassword.text.clear()
            etxtPhoneNumber.text.clear()
        }
    }

    fun togglePasswordVisibility(view: View) {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            binding.etxtPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon)
        } else {
            binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon)
        }

        // Ensure the cursor stays at the end after toggling
        binding.etxtPassword.setSelection(binding.etxtPassword.text.length)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

 private fun setupAutoCompleteForAddress() {
        val autoCompleteAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, placeSuggestions)
        binding.etxtAddress.setAdapter(autoCompleteAdapter)

        binding.etxtAddress.setOnItemClickListener { parent, _, position, _ ->
           val selectedPlace = parent.getItemAtPosition(position) as String
            isAddressValid = true
            findPlace(selectedPlace)
        }

        binding.etxtAddress.addTextChangedListener(object : TextWatcher {
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

    private fun fetchPlaceSuggestions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

       placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            placeSuggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, placeSuggestions)
            binding.etxtAddress.setAdapter(adapter)
           adapter.notifyDataSetChanged()
        }
    }

    private fun findPlace(placeName: String) {
        val geocoder = Geocoder(requireContext())
        val addressList = geocoder.getFromLocationName(placeName, 1)
        if (addressList != null) {
            if (addressList.isNotEmpty()) {
                val address = addressList[0]
                destinationLatLng = LatLng(address.latitude, address.longitude)

            }
        }
    }
}

