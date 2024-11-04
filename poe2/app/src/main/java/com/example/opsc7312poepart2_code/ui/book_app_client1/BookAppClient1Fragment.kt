package com.example.opsc7312poepart2_code.ui.book_app_client1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.google.firebase.database.*
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.opsc7312poepart2_code.ui.AppDatabase
import com.example.opsc7312poepart2_code.ui.MIGRATION_1_2
import com.example.opsc7312poepart2_code.ui.User1
import com.example.opsc7312poepart2_code.ui.UserDao
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.*

class BookAppClient1Fragment : Fragment() {

    private lateinit var dentistList: ArrayList<String>
    private lateinit var listViewAdapter: ArrayAdapter<String>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userDistanceRadius: Double? = null
    private var userDistanceUnit: String = "km" // Default distance unit
    private val TAG = "BookAppClient1Fragment"
    private lateinit var appDatabase: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book_app_client1, container, false)

        val listView = view.findViewById<ListView>(R.id.listofDentists)
        val ibtnMaps = view.findViewById<ImageButton>(R.id.ibtnMaps)
        val ibtnHome = view.findViewById<ImageButton>(R.id.ibtnHome)

        dentistList = ArrayList()
        listViewAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, dentistList)
        listView.adapter = listViewAdapter

        databaseReference = FirebaseDatabase.getInstance().getReference("dentists")
        database = FirebaseDatabase.getInstance().getReference("clients")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        appDatabase = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java, "app_database"
        )
            .addMigrations(MIGRATION_1_2) // Add your migration here
            .build()

        if (isOnline()) {
            // Fetch user settings first
            fetchUserSettings {
                // Once settings are fetched, fetch dentists with location filtering
                fetchDentistsWithLocationFiltering() // Ensure this does not affect list view updates
            }

            // Fetch data from Firebase when online
           syncFirebaseWithRoom {

           }
        } else {
            // Load dentists from Room when offline
            loadDentistsFromRoom()
        }

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedDentist = listViewAdapter.getItem(position)
            if (selectedDentist != null) {
                val bundle = Bundle().apply {
                    putString("selectedDentist", selectedDentist)
                }
                findNavController().navigate(
                    R.id.action_nav_book_app_client1_to_nav_book_app_client2,
                    bundle
                )
            }
        }

        ibtnMaps.setOnClickListener {
            if (isOnline()) {
                findNavController().navigate(R.id.action_nav_book_app_client1_to_nav_dentist_maps)
            } else {
                showToast("No Internet Connection!")
            }
        }
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_book_app_client1_to_nav_menu_client)
        }

        return view
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchUserSettings(onComplete: () -> Unit) {
        // Get the client ID for Firebase
        val clientId = loggedInClientUserId
        // Log.d(TAG, "Fetching settings for Client ID: $clientId") // Log the client ID

        // Log.d(TAG, "Database reference path: clients/$clientId") // Log the reference path

        if (database != null) {
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Log the entire snapshot for debugging purposes
                    // Log.d(TAG, "Snapshot data: ${snapshot.value}")

                    // Check if snapshot exists and if the specific client node exists
                    if (snapshot.exists() && clientId?.let { snapshot.hasChild(it) } == true) {
                        val clientDataSnapshot = clientId?.let { snapshot.child(it) }

                        // Retrieve and trim the distanceRadius value
                        val rawDistanceRadius = clientDataSnapshot?.child("distanceRadius")
                            ?.getValue(String::class.java)
                            ?.trim()
                        userDistanceRadius = when (rawDistanceRadius) {
                            "No Limit" -> null
                            "1" -> 1.0
                            "5" -> 5.0
                            "10" -> 10.0
                            "20" -> 20.0
                            "30" -> 30.0
                            else -> {
                                // Log.d(TAG, "Unrecognized distance radius: $rawDistanceRadius") // Log unrecognized values
                                null
                            }
                        }

                        // Retrieve and trim the distanceUnit value, defaulting to "km"
                        if (clientDataSnapshot != null) {
                            userDistanceUnit = clientDataSnapshot.child("distanceUnit").getValue(String::class.java)?.trim() ?: "km"
                        }

                        // Log the fetched user settings
                        // Log.d(TAG, "User Distance Radius: $userDistanceRadius $userDistanceUnit")
                    } else {
                        // Log.d(TAG, "No data found for client ID: $clientId") // Log if no data exists for the client ID
                    }

                    // Call onComplete to signal that fetching is done
                    onComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Log.e(TAG, "Failed to load user settings: ${error.message}")
                }
            })
        } else {
            // Log.e(TAG, "User reference is null for client ID: $clientId")
            onComplete() // Still call onComplete even if the reference is null
        }
    }

    private fun fetchDentistsWithLocationFiltering() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let { userLocation ->
                val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)
                // Log.d(TAG, "User Location: ${userLatLng.latitude}, ${userLatLng.longitude}")

                databaseReference.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        dentistList.clear() // Clear the previous list
                        var dentistsInRange = 0 // Count of dentists within range

                        for (dentistSnapshot in snapshot.children) {
                            val name = dentistSnapshot.child("name").getValue(String::class.java)
                            val address = dentistSnapshot.child("address").getValue(String::class.java)

                            if (name != null && address != null) {
                                getCoordinatesFromAddress(address)?.let { dentistLocation ->
                                    val distance = calculateDistance(userLatLng, dentistLocation)

                                    // Log distance for debugging
                                    // Log.d(TAG, "Distance to $name: $distance ${userDistanceUnit}")

                                    if (userDistanceRadius == null || distance <= userDistanceRadius!!) {
                                        dentistList.add(name) // Add the dentist to the list
                                        dentistsInRange++
                                    }
                                } // ?: Log.e(TAG, "Could not get coordinates for address: $address")
                            } // else {
                            // Log.e(TAG, "Dentist name or address is null")
                            //}
                        }

                        // Log the number of dentists added
                        // Log.d(TAG, "Total Dentists in Range: $dentistsInRange")
                        listViewAdapter.notifyDataSetChanged() // Notify adapter of data change
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Log.e(TAG, "Database error: ${error.message}")
                    }
                })
            } // ?: Log.e(TAG, "User location is null")
        }
    }

    private fun calculateDistance(userLocation: LatLng, dentistLocation: LatLng): Double {
        val earthRadius = if (userDistanceUnit == "km") 6371 else 3959 // Earth radius in km or miles
        val dLat = Math.toRadians(dentistLocation.latitude - userLocation.latitude)
        val dLon = Math.toRadians(dentistLocation.longitude - userLocation.longitude)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(userLocation.latitude)) *
                cos(Math.toRadians(dentistLocation.latitude)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c // Return the distance in specified units
    }

    private fun getCoordinatesFromAddress(address: String): LatLng? {
        val geocoder = Geocoder(requireContext())
        return try {
            val results = geocoder.getFromLocationName(address, 1)
            if (results?.isNotEmpty() == true) {
                LatLng(results[0].latitude, results[0].longitude)
            } else {
                // Log.e(TAG, "No results found for address: $address")
                null
            }
        } catch (e: IOException) {
            // Log.e(TAG, "Geocoding error: ${e.message}")
            null
        }
    }

    // Check network connectivity
    private fun isOnline(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    // Sync data from Firebase to Room
    private fun syncFirebaseWithRoom(onComplete: () -> Unit) {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Run Room database operations on a background thread
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    // Clear existing dentists in Room to avoid duplicates
                    appDatabase.userDao().clearDentists()

                    val dentistList = mutableListOf<User1>() // List to hold dentists

                    for (dentistSnapshot in snapshot.children) {
                        val name = dentistSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                        val user = User1(userId = null.toString(), username = name, password = null.toString(), role = "dentists")

                        // Insert into Room
                        appDatabase.userDao().insertUser(user)
                        dentistList.add(user) // Add user to the list
                    }

                    // Notify that syncing is done, but do NOT update the UI
                    withContext(Dispatchers.Main) {
                        onComplete() // Signal that syncing is done
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                //Log.e(TAG, "Firebase data fetch failed: ${error.message}")
            }
        })
    }

    private fun loadDentistsFromRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            val users = withContext(Dispatchers.IO) {
                appDatabase.userDao().getUsersByRole() // Fetch dentists from Room
            }

            // Update UI on the main thread
            dentistList.clear()
            dentistList.addAll(users.map { it.username ?: "Unknown" }) // Ensure 'username' is a valid property
            listViewAdapter.notifyDataSetChanged() // Notify the adapter
        }
    }



}

