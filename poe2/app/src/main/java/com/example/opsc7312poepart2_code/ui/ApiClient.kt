package com.example.opsc7312poepart2_code.ui

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://us-central1-opsc7312database.cloudfunctions.net/api/"

    // Token-based interceptor with context passed as parameter
    fun createApiService(context: Context): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val token = getTokenFromSharedPreferences(context)
                Log.d("TokenDebug", "Token retrieved for request: $token") // Log the token being used
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    // Retrieves token from shared preferences
    public fun getTokenFromSharedPreferences(context: Context): String? {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPref.getString("jwt_token", null)
        Log.d("TokenDebug", "Token retrieved from shared preferences: $token") // Log the retrieved token
        return token
    }
}




