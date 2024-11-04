package com.example.opsc7312poepart2_code.ui.forget_password_client

import android.os.Bundle
import android.text.InputType
import android.util.Base64
// import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.poe2.databinding.FragmentForgetPasswordClientBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest
import java.security.SecureRandom

class ForgetPasswordClientFragment : Fragment() {
    private var _binding: FragmentForgetPasswordClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference

    private var passwordVisible = false // Password visibility state

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgetPasswordClientBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("clients") // Reference to the clients node in the database

        // Set up the save button click listener
        binding.btnSave.setOnClickListener {
            val username = binding.etxtUsername.text.toString().trim()
            val newPassword = binding.etxtNewPassword.text.toString().trim()
            val email = binding.etxtEmail.text.toString().trim()

            // Check if all fields are filled
            if (username.isNotEmpty() && newPassword.isNotEmpty() && email.isNotEmpty()) {
                resetPassword(username, email, newPassword)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up the cancel button click listener
        binding.btnCancel.setOnClickListener {
            // Handle cancel button click
            requireActivity().onBackPressed()
        }

        // Set the password field to not visible by default
        binding.etxtNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Set up password visibility toggle
        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility(it)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding reference to avoid memory leaks
    }

    // Function to toggle password visibility
    fun togglePasswordVisibility(view: View) {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            binding.etxtNewPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon) // Change to visible icon
        }
        binding.etxtNewPassword.setSelection(binding.etxtNewPassword.text.length) // Set cursor to end
    }

    // Function to reset the password
    private fun resetPassword(username: String, email: String, newPassword: String) {
        // Log.d("ForgetPasswordDentistFragment", "Attempting to reset password for username: $username")
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val storedEmail = userSnapshot.child("email").getValue(String::class.java)

                    // Check if the provided email matches the stored email
                    if (storedEmail == email) {
                        // Log.d("ForgetPasswordDentistFragment", "Email matched, proceeding to reset password.")
                        // Hash and salt the new password
                        val newSalt = generateSalt()
                        val hashedNewPassword = hashPassword(newPassword, newSalt)

                        // Update the user's password and salt in the database
                        userSnapshot.ref.child("password").setValue(hashedNewPassword)
                        userSnapshot.ref.child("salt").setValue(Base64.encodeToString(newSalt, Base64.DEFAULT))
                        userSnapshot.ref.child("isPasswordUpdated").setValue(true) // Set the updated flag to true

                        Toast.makeText(requireContext(), "Password reset successfully!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_nav_forget_password_client_to_nav_login_client)
                    } else {
                        Toast.makeText(requireContext(), "Email does not match the username.", Toast.LENGTH_SHORT).show()
                        // Log.d("ForgetPasswordDentistFragment", "Email does not match for username: $username")
                    }
                } else {
                    Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                    // Log.d("ForgetPasswordDentistFragment", "User not found for username: $username")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                // Log.e("ForgetPasswordDentistFragment", "Database error: ${error.message}")
            }
        })
    }

    // Function to generate a salt for password hashing
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        // Log.d("ForgetPasswordDentistFragment", "Generated salt: ${Base64.encodeToString(salt, Base64.DEFAULT)}")
        return salt
    }
    // The code above was taken and adapted from StackOverflow
    // https://stackoverflow.com/questions/78309846/javax-crypto-aeadbadtagexception-bad-decrypt-in-aes256-decryption
    // Jagar
    // https://stackoverflow.com/users/12053756/jagar

    // Function to hash the password using SHA-256
    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashedPassword = Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
        // Log.d("ForgetPasswordDentistFragment", "Hashed password: $hashedPassword")
        return hashedPassword
    }
    // The code above was taken and adapted from Hyperskill
    // https://hyperskill.org/learn/step/36628
}
s