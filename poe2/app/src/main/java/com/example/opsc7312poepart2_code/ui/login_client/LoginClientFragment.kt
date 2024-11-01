package com.example.opsc7312poepart2_code.ui.login_client

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
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment.Companion.loggedInDentistUserId
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
import java.security.MessageDigest
import java.util.Date

class LoginClientFragment : Fragment() {

    private var _binding: FragmentLoginClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val RC_SIGN_IN = 9001
    private lateinit var mGoogleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    private var passwordVisible = false // Password visibility state

    companion object {
        var loggedInClientUsername: String? = null // Global variable to store the logged-in username
        var loggedInClientUserId: String? = null // Global variable to store the logged-in user ID
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginClientBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Database and Auth
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("clients")
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
            togglePasswordVisibility()
        }

        // Handle Forget Password text click
        binding.txtForgotPassword.setOnClickListener {
            Log.d("LoginClientFragment", "Forget Password text clicked")
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
        val username = binding.etxtUsername.text.toString().trim()

        if (username.isEmpty()) {
            showToast("Error: Username is required.")
            return
        }

        dbReference.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.e("LoginClientFragment", "User $username not found in the database")
                        showToast("Error: User not found.")
                        return
                    }

                    Log.d("LoginClientFragment", "User $username found in the database")
                    val userSnapshot = snapshot.children.first()
                    val userId = userSnapshot.child("id").getValue(String::class.java).orEmpty()
                    val role = userSnapshot.child("role").getValue(String::class.java).orEmpty().ifEmpty { "client" }

                    loggedInClientUsername = username
                    //loggedInClientUserId = userId

                    loggedInClientUserId = snapshot.children.first().key // Get user ID

                    Log.d("LoginDentistFragment", "Id is: $loggedInClientUserId")
                    Log.d("LoginDentistFragment", "Id is: $username")
                    getUserIdFromFirebase(username)



                    val biometricAuthenticator = BiometricAuthenticator(requireActivity(), {
                        Log.d("LoginClientFragment", "Biometric authentication successful for user: $username")

                        val jwtToken = generateJwtToken(userId, role)
                        Log.d("LoginClientFragment", "JWT Token generated: $jwtToken")

                        Log.d("LoginClientFragment", "Navigating to client menu after successful authentication")
                        findNavController().navigate(R.id.action_nav_login_client_to_nav_menu_client)
                    }, { errorMessage ->
                        showToast(errorMessage)
                        Log.e("LoginClientFragment", "Biometric authentication error: $errorMessage")
                    })

                    Log.d("LoginClientFragment", "Starting biometric authentication process")
                    biometricAuthenticator.authenticate()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LoginClientFragment", "Database error: ${error.message}")
                    showToast("Error: Database connection failed.")
                }
            })

    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            Log.d("LoginClientFragment", "Google Sign-In request code received")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginClientFragment", "Google Sign-In successful for user: ${account.displayName}")
                showToast("Sign-in successful.")
                findNavController().navigate(R.id.action_nav_login_client_to_nav_menu_client)
            } catch (e: ApiException) {
                Log.e("LoginClientFragment", "Google Sign-In failed: ${e.statusCode}")
                showToast("Sign-in failed: ${e.statusCode}")
            }
        }
    }

    fun onForgotPasswordClicked() {
        Log.d("LoginClientFragment", "Navigating to Forget Password Fragment")
        findNavController().navigate(R.id.action_nav_login_client_to_nav_forget_password_client)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("LoginClientFragment", "Fragment view destroyed")
        _binding = null
    }

    private fun loginUser(username: String, password: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("LoginClientFragment", "User $username found in the database")
                    val userSnapshot = snapshot.children.first()
                    val storedHashedPassword = userSnapshot.child("password").getValue(String::class.java) ?: ""
                    val storedSalt = userSnapshot.child("salt").getValue(String::class.java)?.let { Base64.decode(it, Base64.DEFAULT) } ?: ByteArray(0)
                    val userId = userSnapshot.child("id").getValue(String::class.java) ?: "" // Retrieve the user ID
                    val role = userSnapshot.child("role").getValue(String::class.java) ?: "client" // Assuming default role is "client"

                    // Hash the entered password
                    val hashedPassword = hashPassword(password, storedSalt)

                    // Compare hashed password with the stored password
                    if (hashedPassword == storedHashedPassword) {
                        Log.d("LoginClientFragment", "Password matches for user: $username")
                        loggedInClientUsername = username
                        loggedInClientUserId = userId

                        Log.d("LoginDentistFragment", "Id is: $loggedInClientUserId")
                        Log.d("LoginDentistFragment", "Id is: $userId")
                        getUserIdFromFirebase(username) // Fetch and store user ID

                        // Generate JWT token with ID and role
                        val jwtToken = generateJwtToken(userId, role)
                        saveToken(jwtToken) // Save the generated token
                        Log.d("LoginClientFragment", "JWT Token generated: $jwtToken")

                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_nav_login_client_to_nav_menu_client)
                    } else {
                        Log.d("LoginClientFragment", "Incorrect password for user: $username")
                        Toast.makeText(requireContext(), "Incorrect password.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("LoginClientFragment", "User $username not found in the database")
                    Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginClientFragment", "Database error during login: ${error.message}")
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun saveToken(token: String) {
        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("jwt_token", token).apply()
        Log.d("TokenDebug", "Token saved: $token") // Log the token when saved
    }

    private fun generateJwtToken(id: String, role: String): String {
        Log.d("LoginClientFragment", "Generating JWT Token for ID: $id with role: $role")
        val algorithm = Algorithm.HMAC256("supersecretkey")

        val token = JWT.create()
            .withIssuer("auth0")
            .withClaim("id", id)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(algorithm)

        saveToken(token) // Save token after generation
        return token
    }


    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        binding.etxtPassword.inputType = if (passwordVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etxtPassword.setSelection(binding.etxtPassword.text.length) // Set cursor at the end
        binding.iconViewPassword.setImageResource(if (passwordVisible) R.drawable.visible_icon else R.drawable.visible_icon)
        Log.d("LoginClientFragment", "Password visibility toggled to: $passwordVisible")
    }

    private fun getUserIdFromFirebase(username: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    loggedInClientUserId = snapshot.children.first().key // Get user ID
                    Log.d("LoginClientFragment", "Logged in user ID: $loggedInClientUserId")
                } else {
                    Log.d("LoginClientFragment", "User ID not found for $username")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginClientFragment", "Database error when retrieving user ID: ${error.message}")
            }
        })
    }
}
