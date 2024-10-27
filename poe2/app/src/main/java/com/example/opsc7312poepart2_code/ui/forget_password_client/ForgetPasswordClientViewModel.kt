package com.example.opsc7312poepart2_code.ui.forget_password_client

import android.util.Log
import androidx.lifecycle.ViewModel

class ForgetPasswordClientViewModel : ViewModel() {
    private var registeredUsers: HashMap<String, Triple<String, String, String>> = hashMapOf()

    fun setRegisteredUserCredentials(username: String, email: String, password: String) {
        registeredUsers[username] = Triple(email.trim(), password.trim(), "") // Added a third field for the email
        Log.d("ForgetPasswordViewModel", "Registered User: $username, Email: $email, Password: $password")
    }

    fun isValidUser(username: String, email: String): Boolean {
        val enteredEmail = email.trim()
        val isValid = registeredUsers.containsKey(username) && registeredUsers[username]?.first == enteredEmail
        Log.d("ForgetPasswordViewModel", "Checking if user is valid: $isValid")
        return isValid
    }

    fun updatePassword(username: String, newPassword: String): Boolean {
        if (registeredUsers.containsKey(username)) {
            val email = registeredUsers[username]?.first ?: ""
            val oldPassword = registeredUsers[username]?.second ?: ""
            registeredUsers[username] = Triple(email, newPassword.trim(), oldPassword)
            Log.d("ForgetPasswordViewModel", "Updated Password for $username: $newPassword")
            return true
        }
        return false
    }

    fun getPassword(username: String): String? {
        val password = registeredUsers[username]?.second
        Log.d("ForgetPasswordViewModel", "Current Password for $username: $password")
        return password
    }

    fun getEmail(username: String): String? {
        return registeredUsers[username]?.first
    }
}