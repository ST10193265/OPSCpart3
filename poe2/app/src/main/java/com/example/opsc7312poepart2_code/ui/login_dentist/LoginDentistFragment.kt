package com.example.opsc7312poepart2_code.ui.login_dentist

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
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
import androidx.room.Room
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.opsc7312poepart2_code.ui.AppDatabase
import com.example.opsc7312poepart2_code.ui.BiometricAuthenticator
import com.example.opsc7312poepart2_code.ui.MIGRATION_1_2
import com.example.opsc7312poepart2_code.ui.User1
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUsername
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Date

class LoginDentistFragment : Fragment() {

    private var _binding: FragmentLoginDentistBinding? = null
    private val binding get() = _binding!!
    private lateinit var appDatabase: AppDatabase
    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val RC_SIGN_IN = 9001
    private lateinit var mGoogleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    private var passwordVisible = false // Password visibility state

    companion object {
        var loggedInDentistUsername: String? = null // Global variable to store the logged-in username
        var loggedInDentistUserId: String? = null // Global variable to store the logged-in user ID
        var isBiometricLogin: Boolean = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginDentistBinding.inflate(inflater, container, false)
        val root: View = binding.root

        appDatabase = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java, "app_database"
        )
            .addMigrations(MIGRATION_1_2) // Add your migration here
            .build()
        // Initialize Firebase Database and Auth
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("dentists")
        auth = FirebaseAuth.getInstance()

      //  Log.d("LoginDentistFragment", "Firebase initialized, Auth and Database setup complete")

        // Handle login button click
        binding.btnLogin.setOnClickListener {
            val username = binding.etxtUsername.text.toString().trim()
            val password = binding.etxtPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                if (isOnline()) {
                loginUser(username, password)
                    } else {
                    loginUserOffline(username, password)
                }

            } else {
                showToast("Please enter both username and password.")
            }
        }

        binding.btnBiometricLogin.setOnClickListener {
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
            signIn()
        }

        return root
    }

    // Initiate Google Sign-In
    private fun signIn() {
        if (isOnline()) {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }else {
            showToast("No Internet Connection!")

        }
    }
    private fun initiateBiometricLogin() {
        if (isOnline()) {
            val username = binding.etxtUsername.text.toString().trim()


        dbReference.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                     //   Log.e("LoginDentistFragment", "User $username not found in the database")
                        showToast("Error: User not found.")
                        return
                    }

                  //  Log.d("LoginDentistFragment", "User $username found in the database")
                    val userSnapshot = snapshot.children.first()
                    var userId = userSnapshot.child("id").getValue(String::class.java).orEmpty()
                    val role = userSnapshot.child("role").getValue(String::class.java).orEmpty().ifEmpty { "dentist" }

                    loggedInDentistUsername = username
                    loggedInDentistUserId = snapshot.children.first().key // Get user ID
                    //getUserIdFromFirebase(username) // Fetch and store user ID
                    userId = loggedInDentistUserId.toString()

                  //  Log.d("LoginDentistFragment", "loggedInDentistUserId is: $loggedInDentistUserId")
                  //  Log.d("LoginDentistFragment", "userId is: $userId")

                    val biometricAuthenticator = BiometricAuthenticator(requireActivity(), {
                       // Log.d("LoginDentistFragment", "Biometric authentication successful for user: $username")

                        isBiometricLogin = true
                      //  Log.d("LoginDentistFragment", "isBiometricLogin: $isBiometricLogin")

                        val jwtToken = generateJwtToken(userId, role)
                      //  Log.d("LoginDentistFragment", "JWT Token generated: $jwtToken")

                      //  Log.d("LoginDentistFragment", "Navigating to client menu after successful authentication")
                        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
                    }, { errorMessage ->
                        showToast(errorMessage)
                      //  Log.e("LoginDentistFragment", "Biometric authentication error: $errorMessage")
                    })

                   // Log.d("LoginDentistFragment", "Starting biometric authentication process")
                    biometricAuthenticator.authenticate()
                }

                override fun onCancelled(error: DatabaseError) {
                   // Log.e("LoginDentistFragment", "Database error: ${error.message}")
                    showToast("Error: Database connection failed.")
                }
            })

            if (username.isEmpty()) {
                showToast("Error: Username is required.")
                return
            }

            dbReference.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            Log.e(
                                "LoginDentistFragment",
                                "User $username not found in the database"
                            )
                            showToast("Error: User not found.")
                            return
                        }

                        Log.d("LoginDentistFragment", "User $username found in the database")
                        val userSnapshot = snapshot.children.first()
                        var userId = userSnapshot.child("id").getValue(String::class.java).orEmpty()
                        val role = userSnapshot.child("role").getValue(String::class.java).orEmpty()
                            .ifEmpty { "dentist" }

                        loggedInDentistUsername = username
                        loggedInDentistUserId = snapshot.children.first().key // Get user ID
                        //getUserIdFromFirebase(username) // Fetch and store user ID
                        userId = loggedInDentistUserId.toString()

                        Log.d(
                            "LoginDentistFragment",
                            "loggedInDentistUserId is: $loggedInDentistUserId"
                        )
                        Log.d("LoginDentistFragment", "userId is: $userId")

                        val biometricAuthenticator = BiometricAuthenticator(requireActivity(), {
                            Log.d(
                                "LoginDentistFragment",
                                "Biometric authentication successful for user: $username"
                            )

                            isBiometricLogin = true
                            Log.d("LoginDentistFragment", "isBiometricLogin: $isBiometricLogin")

                            val jwtToken = generateJwtToken(userId, role)
                            Log.d("LoginDentistFragment", "JWT Token generated: $jwtToken")

                            Log.d(
                                "LoginDentistFragment",
                                "Navigating to client menu after successful authentication"
                            )
                            findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
                        }, { errorMessage ->
                            showToast(errorMessage)
                            Log.e(
                                "LoginDentistFragment",
                                "Biometric authentication error: $errorMessage"
                            )
                        })

                        Log.d("LoginDentistFragment", "Starting biometric authentication process")
                        biometricAuthenticator.authenticate()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("LoginDentistFragment", "Database error: ${error.message}")
                        showToast("Error: Database connection failed.")
                    }
                })
        }else {
            showToast("No Internet Connection!")


        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                showToast("Sign-in successful.")
                findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
            } catch (e: ApiException) {
                showToast("Sign-in failed: ${e.statusCode}")
            }
        }
    }

    fun onForgotPasswordClicked() {
        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_forget_password_dentist)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loginUser(username: String, password: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val storedHashedPassword = userSnapshot.child("password").getValue(String::class.java) ?: ""
                    val storedSalt = userSnapshot.child("salt").getValue(String::class.java)?.let { Base64.decode(it, Base64.DEFAULT) } ?: ByteArray(0)
                    val userId = userSnapshot.child("id").getValue(String::class.java) ?: ""
                    val role = userSnapshot.child("role").getValue(String::class.java) ?: "dentist"

                    val hashedPassword = hashPassword(password, storedSalt)

                    if (hashedPassword == storedHashedPassword) {
                        loggedInDentistUsername = username
                        getUserIdFromFirebase(username) // Fetch and store user ID

                        loggedInDentistUserId = userId

                        // Generate JWT token with ID and role
                        val jwtToken = generateJwtToken(userId, role)
                        saveToken(jwtToken) // Save the generated token
                      //  Log.d("LoginDentistFragment", "JWT Token generated: $jwtToken")

                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        saveUserToLocalDatabase(userId, username, password, role)
                        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
                    } else {
                        showToast("Incorrect password.")
                    }
                } else {
                    showToast("User not found.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Error: ${error.message}")
            }
        })
    }

    private fun getUserIdFromFirebase(username: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    loggedInDentistUserId = snapshot.children.first().key // Get user ID
                  //  Log.d("LoginDentistFragment", "Logged in user ID: $loggedInDentistUserId")
                } else {
                  //  Log.d("LoginDentistFragment", "User ID not found for $username")
                }
            }

            override fun onCancelled(error: DatabaseError) {
               // Log.e("LoginDentistFragment", "Database error when retrieving user ID: ${error.message}")
            }
        })
    }

    private fun saveToken(token: String) {
        val sharedPref = requireActivity().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("jwt_token", token).apply()
      //  Log.d("TokenDebug", "Token saved: $token") // Log the token when saved
    }

    private fun generateJwtToken(id: String, role: String): String {
       // Log.d("LoginClientFragment", "Generating JWT Token for ID: $id with role: $role")
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
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(salt)
        val hashedBytes = messageDigest.digest(password.toByteArray())
        return Base64.encodeToString(hashedBytes, Base64.DEFAULT)
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

    // Offline login using local Room database
    private fun loginUserOffline(username: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val user = appDatabase.userDao()?.getUserByUsername(username)
            if (user != null) {
                if (password == user.password) { // Ensure password check logic matches your setup
                    loggedInClientUsername = username
                    loggedInClientUserId = user.userId // Get userId from the User1 object
                    withContext(Dispatchers.Main) {
                        showToast("Offline login successful!")
                        findNavController().navigate(R.id.action_nav_login_client_to_nav_menu_client)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Incorrect password.")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    showToast("User not found.")
                }
            }
        }
    }


    // Check network connectivity
    private fun isOnline(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun saveUserToLocalDatabase(userId: String, username: String, password: String, role: String) {
        val user = User1(userId = userId, username = username, password = password, role = role)
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.userDao()?.insertUser(user)
            Log.d("LoginClientFragment", "User saved to local database: $username")
        }
    }
}
