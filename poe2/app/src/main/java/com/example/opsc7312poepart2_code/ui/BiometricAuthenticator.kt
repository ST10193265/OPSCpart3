package com.example.opsc7312poepart2_code.ui

import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricAuthenticator(
    private val activity: FragmentActivity,
    private val onSuccess: () -> Unit,
    private val onError: (String) -> Unit = {}
) {

    private val executor: Executor = ContextCompat.getMainExecutor(activity)
    private val biometricPrompt: BiometricPrompt

    init {
        biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                // Handle successful authentication
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Handle error
                Log.e("BiometricAuthenticator", "Authentication error: $errString")
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                // Handle failure
                Log.e("BiometricAuthenticator", "Authentication failed")
                onError("Authentication failed")
            }
        })
    }

    fun authenticate() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
