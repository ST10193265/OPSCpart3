package com.example.opsc7312poepart2_code.ui.login_dentist

import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.opsc7312poepart2_code.ui.BiometricAuthenticator
import com.example.poe2.R
import com.example.poe2.databinding.FragmentLoginDentistBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest
import java.util.Date

class LoginDentistFragment : Fragment() {

    private var _binding: FragmentLoginDentistBinding? = null
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
        _binding = FragmentLoginDentistBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Database and Auth
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("dentists")
        auth = FirebaseAuth.getInstance()

        Log.d("LoginDentistFragment", "Firebase initialized, Auth and Database setup complete")

        // Handle login button click
        binding.btnLogin.setOnClickListener {
            val username = binding.etxtUsername.text.toString().trim()
            val password = binding.etxtPassword.text.toString().trim()

            Log.d("LoginDentistFragment", "Login button clicked")

            if (username.isNotEmpty() && password.isNotEmpty()) {
                Log.d("LoginDentistFragment", "Attempting to login user: $username")
                loginUser(username, password)
            } else {
                showToast("Please enter both username and password.")
                Log.d("LoginDentistFragment", "Username or password was empty")
            }
        }

        binding.btnBiometricLogin.setOnClickListener {
            Log.d("LoginDentistFragment", "Biometric login button clicked")
            initiateBiometricLogin()
        }

        // Set the password field to not visible by default
        binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Handle password visibility toggle
        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Handle Forget Password text click
        binding.txtForgotPassword.setOnClickListener {
            Log.d("LoginDentistFragment", "Forget Password text clicked")
            onForgotPasswordClicked()
        }

        // Initialize Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Initialize Google Sign-In client
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Bind the Sign-In button and set up a click listener
        binding.btnGoogleSignIn.setOnClickListener {
            Log.d("LoginDentistFragment", "Google Sign-In button clicked")
            signIn()
        }

        return root
    }

    // Initiate Google Sign-In
    private fun signIn() {
        Log.d("LoginDentistFragment", "Starting Google Sign-In")
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun initiateBiometricLogin() {
        Log.d("LoginDentistFragment", "Initiating biometric login")
        val biometricAuthenticator = BiometricAuthenticator(requireActivity(), {
            val username = binding.etxtUsername.text.toString().trim()
            Log.d("LoginDentistFragment", "Biometric authentication successful for user: $username")

            if (username.isNotEmpty()) {
                val jwtToken = generateJwtToken(username, "dentist") // Assuming role is "dentist"
                Log.d("LoginDentistFragment", "JWT Token generated: $jwtToken")
                findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
            } else {
                Log.e("LoginDentistFragment", "Username is empty after biometric authentication")
                showToast("Error: Username is required.")
            }
        }, { errorMessage ->
            showToast(errorMessage)
            Log.e("LoginDentistFragment", "Biometric authentication error: $errorMessage")
        })
        biometricAuthenticator.authenticate()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            Log.d("LoginDentistFragment", "Google Sign-In request code received")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginDentistFragment", "Google Sign-In successful for user: ${account.displayName}")
                showToast("Sign-in successful.")
                findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
            } catch (e: ApiException) {
                Log.e("LoginDentistFragment", "Google Sign-In failed: ${e.statusCode}")
                showToast("Sign-in failed: ${e.statusCode}")
            }
        }
    }

    fun onForgotPasswordClicked() {
        Log.d("LoginDentistFragment", "Navigating to Forget Password Fragment")
        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_forget_password_dentist)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("LoginDentistFragment", "Fragment view destroyed")
        _binding = null
    }

    private fun loginUser(username: String, password: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("LoginDentistFragment", "User $username found in the database")
                    val userSnapshot = snapshot.children.first()
                    val storedHashedPassword = userSnapshot.child("password").getValue(String::class.java) ?: ""
                    val storedSalt = userSnapshot.child("salt").getValue(String::class.java)?.let { Base64.decode(it, Base64.DEFAULT) } ?: ByteArray(0)
                    val userId = userSnapshot.child("id").getValue(String::class.java) ?: "" // Retrieve the user ID
                    val role = userSnapshot.child("role").getValue(String::class.java) ?: "dentist"

                    val hashedPassword = hashPassword(password, storedSalt)

                    if (hashedPassword == storedHashedPassword) {
                        Log.d("LoginDentistFragment", "Password matches for user: $username")
                        loggedInDentistUsername = username
                        getUserIdFromFirebase(username)

                        val jwtToken = generateJwtToken(userId, role)
                        saveToken(jwtToken)
                        Log.d("LoginDentistFragment", "JWT Token generated: $jwtToken")

                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
                    } else {
                        Log.d("LoginDentistFragment", "Incorrect password for user: $username")
                        Toast.makeText(requireContext(), "Incorrect password.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("LoginDentistFragment", "User $username not found in the database")
                    Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginDentistFragment", "Database error during login: ${error.message}")
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveToken(token: String) {
        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("auth_token", token).apply()
        Log.d("LoginDentistFragment", "JWT token saved to shared preferences")
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(salt)
        val hashedBytes = messageDigest.digest(password.toByteArray())
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT)
    }

    private fun generateJwtToken(userId: String, role: String): String {
        val algorithm = Algorithm.HMAC256("your_secret_key")
        return JWT.create()
            .withIssuer("auth0")
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // Token expires in 1 hour
            .sign(algorithm)
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        binding.etxtPassword.inputType = if (passwordVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etxtPassword.setSelection(binding.etxtPassword.text.length)
    }

    private fun getUserIdFromFirebase(username: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    loggedInDentistUserId = userSnapshot.child("id").getValue(String::class.java)
                    Log.d("LoginDentistFragment", "User ID retrieved from Firebase: $loggedInDentistUserId")
                } else {
                    Log.d("LoginDentistFragment", "User ID not found for user: $username")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginDentistFragment", "Error retrieving user ID from Firebase: ${error.message}")
            }
        })
    }
}
