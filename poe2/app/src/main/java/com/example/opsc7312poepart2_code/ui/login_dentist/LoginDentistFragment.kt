package com.example.opsc7312poepart2_code.ui.login_dentist

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.poe2.databinding.FragmentLoginClientBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.opsc7312poepart2_code.ui.BiometricAuthenticator
import java.security.MessageDigest
import java.util.*

class LoginDentistFragment : Fragment() {

    private var _binding: FragmentLoginClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val RC_SIGN_IN = 9001
    private lateinit var mGoogleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    private var passwordVisible = false // Password visibility state

    companion object {
        var loggedInDentistUsername: String? = null // Global variable to store the logged-in username
        var loggedInDentistUserId: String? = null // Global variable to store the logged-in user ID
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginClientBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Database and Auth
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("dentists")
        auth = FirebaseAuth.getInstance()

        Log.d("LoginClientFragment", "Firebase initialized, Auth and Database setup complete")

        // Handle login button click
        binding.btnLogin.setOnClickListener {
            val username = binding.etxtUsername.text.toString().trim()
            val password = binding.etxtPassword.text.toString().trim()

            Log.d("LoginClientFragment", "Login button clicked")

            if (username.isNotEmpty() && password.isNotEmpty()) {
                Log.d("LoginClientFragment", "Attempting to login user: $username")
                loginUser(username, password)
            } else {
                showToast("Please enter both username and password.")
                Log.d("LoginClientFragment", "Username or password was empty")
            }
        }

        binding.btnBiometricLogin.setOnClickListener {
            Log.d("LoginClientFragment", "Biometric login button clicked")
            initiateBiometricLogin()
        }

        // Set the password field to not visible by default
        binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Handle password visibility toggle
        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility(it)
        }

        // Handle Forget Password text click
        binding.txtForgotPassword.setOnClickListener {
            Log.d("LoginClientFragment", "Forget Password text clicked")
            onForgotPasswordClicked(it)
        }

        // Initialize Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Initialize Google Sign-In client
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Bind the Sign-In button and set up a click listener
        binding.btnGoogleSignIn.setOnClickListener {
            Log.d("LoginClientFragment", "Google Sign-In button clicked")
            signIn()
        }

        return root
    }

    // Initiate Google Sign-In
    private fun signIn() {
        Log.d("LoginClientFragment", "Starting Google Sign-In")
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun initiateBiometricLogin() {
        Log.d("LoginClientFragment", "Initiating biometric login")
        val biometricAuthenticator = BiometricAuthenticator(requireActivity(), {
            // On success: Retrieve username from the database
            val username = binding.etxtUsername.text.toString().trim() // Assuming you can retrieve the username from the UI
            Log.d("LoginClientFragment", "Biometric authentication successful for user: $username")

            if (username.isNotEmpty()) {
                // Generate JWT token
                val jwtToken = generateJwtToken(username)
                Log.d("LoginClientFragment", "JWT Token generated: $jwtToken")
                // Proceed to client menu
                findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
            } else {
                Log.e("LoginClientFragment", "Username is empty after biometric authentication")
                showToast("Error: Username is required.")
            }
        }, { errorMessage ->
            // Handle errors
            showToast(errorMessage) // For example, show an error toast
            Log.e("LoginClientFragment", "Biometric authentication error: $errorMessage")
        })
        biometricAuthenticator.authenticate()
    }


    // Generate a JWT token for the logged-in user
    private fun generateJwtToken(username: String?): String {
        if (username == null) {
            Log.d("LoginClientFragment", "Username is null, cannot generate JWT Token")
            return ""
        }

        Log.d("LoginClientFragment", "Generating JWT Token for username: $username")

        val algorithm = Algorithm.HMAC256("secret") // Use a strong secret in production
        val expirationTime = System.currentTimeMillis() + 3600000
        val token = JWT.create()
            .withIssuer("auth0")
            .withClaim("username", username)
            .withExpiresAt(Date(expirationTime)) // Token expires in 1 hour
            .sign(algorithm)

        Log.d("LoginClientFragment", "JWT Token generated successfully for $username")
        Log.d("LoginClientFragment", "Token expiration time: $expirationTime (1 hour from generation)")
        Log.d("LoginClientFragment", "Generated JWT Token: $token")
        return token
    }


    private fun showToast(message: String) {
        // Show a Toast message for feedback
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Handle the result of Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            Log.d("LoginClientFragment", "Google Sign-In request code received")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginClientFragment", "Google Sign-In successful for user: ${account.displayName}")
                showToast("Sign-in successful.")
                findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
            } catch (e: ApiException) {
                Log.e("LoginClientFragment", "Google Sign-In failed: ${e.statusCode}")
                showToast("Sign-in failed: ${e.statusCode}")
            }
        }
    }

    // Navigate to the Forget Password Fragment
    fun onForgotPasswordClicked(view: View) {
        Log.d("LoginClientFragment", "Navigating to Forget Password Fragment")
        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_forget_password_dentist)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("LoginClientFragment", "Fragment view destroyed")
        _binding = null
    }

    // Authenticate the user
    private fun loginUser(username: String, password: String) {
        Log.d("LoginClientFragment", "Attempting to login user: $username")
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("LoginClientFragment", "User $username found in the database")
                    val userSnapshot = snapshot.children.first()
                    val storedHashedPassword = userSnapshot.child("password").getValue(String::class.java) ?: ""
                    val storedSalt = userSnapshot.child("salt").getValue(String::class.java)?.let { Base64.decode(it, Base64.DEFAULT) } ?: ByteArray(0)

                    // Hash the entered password
                    val hashedPassword = hashPassword(password, storedSalt)

                    // Compare hashed password with the stored password
                    if (hashedPassword == storedHashedPassword) {
                        Log.d("LoginClientFragment", "Password matches for user: $username")
                        loggedInDentistUsername = username
                        getUserIdFromFirebase(username)
                        generateJwtToken(username)
                        showToast("Login successful!")
                        clearFields()
                        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
                    } else {
                        Log.d("LoginClientFragment", "Incorrect password for user: $username")
                        showToast("Incorrect password.")
                    }
                } else {
                    Log.d("LoginClientFragment", "User $username not found in the database")
                    showToast("User not found.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginClientFragment", "Database error: ${error.message}")
            }
        })
    }




    // Hash the password
    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
    }

    // Get user ID from Firebase
    private fun getUserIdFromFirebase(username: String) {
        Log.d("LoginClientFragment", "Fetching user ID for: $username")
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    loggedInDentistUserId = snapshot.children.first().key // Store the user ID globally
                    Log.d("LoginClientFragment", "User ID retrieved: $loggedInDentistUserId")
                } else {
                    Log.d("LoginClientFragment", "No user ID found for: $username")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginClientFragment", "Database error while fetching user ID: ${error.message}")
            }
        })
    }

    private fun clearFields() {
        binding.etxtUsername.text.clear()
        binding.etxtPassword.text.clear()
        Log.d("LoginClientFragment", "Cleared input fields")
    }

    // Toggle password visibility
    private fun togglePasswordVisibility(view: View) {
        passwordVisible = !passwordVisible // Toggle the password visibility state
        binding.etxtPassword.inputType = if (passwordVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etxtPassword.setSelection(binding.etxtPassword.text.length) // Set cursor to the end of the text
        Log.d("LoginClientFragment", "Password visibility toggled: $passwordVisible")
    }
}
