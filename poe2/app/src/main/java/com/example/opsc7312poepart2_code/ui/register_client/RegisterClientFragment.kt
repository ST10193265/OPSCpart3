package com.example.opsc7312poepart2_code.ui.register_client

import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.poe2.databinding.FragmentRegisterClientBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest
import java.security.SecureRandom

class RegisterClientFragment : Fragment() {

    private var _binding: FragmentRegisterClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference

    private var passwordVisible = false // Track password visibility state

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterClientBinding.inflate(inflater, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("clients")

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


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onRegisterClick() {
        val name = binding.etxtName.text.toString().trim()
        val surname = binding.etxtSurname.text.toString().trim()
        val email = binding.etxtEmail.text.toString().trim()
        val username = binding.etxtUsername.text.toString().trim()
        val password = binding.etxtPassword.text.toString().trim()
        val phoneNumber = binding.etxtPhoneNumber.text.toString().trim()

        if (isValidInput(name, surname, email, username, password, phoneNumber)) {
            registerUser(name, surname, email, username, password, phoneNumber)
        } else {
            Toast.makeText(requireContext(), "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidInput(
        name: String, surname: String, email: String, username: String, password: String, phoneNumber: String
    ): Boolean {
        if (name.isEmpty()) {
            showToastMessage("Please enter your first name.")
            return false
        }

        if (surname.isEmpty()) {
            showToastMessage("Please enter your surname.")
            return false
        }

        if (email.isEmpty()) {
            showToastMessage("Please enter your email.")
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToastMessage("Please enter a valid email address.")
            return false
        }

        if (username.isEmpty()) {
            showToastMessage("Please enter your username.")
            return false
        }

        if (password.length < 6) {
            showToastMessage("Password must be at least 6 characters long.")
            return false
        }

        if (phoneNumber.isEmpty()) {
            showToastMessage("Please enter your phone number.")
            return false
        }

        // If all validations pass
        return true
    }

    // Helper function to show a Toast message
    private fun showToastMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }



    private fun registerUser(
        name: String, surname: String, email: String, username: String, password: String, phoneNumber: String
    ) {
        val userId = dbReference.push().key ?: return showToast("Failed to generate user ID")

        // Hash and salt the password
        val salt = generateSalt()
        val hashedPassword = hashPassword(password, salt)

        val user = hashMapOf(
            "userId" to userId,
            "name" to name,
            "surname" to surname,
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
                findNavController().navigate(R.id.action_nav_register_client_to_nav_login_client)
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
            etxtSurname.text.clear()
            etxtEmail.text.clear()
            etxtUsername.text.clear()
            etxtPassword.text.clear()
            etxtPhoneNumber.text.clear()
        }
    }

    // Ensure this method is public
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
}
