package com.example.opsc7312poepart2_code.ui.clients_dentist_map

import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import android.Manifest
import com.example.poe2.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.util.Locale

class ClientDentistMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var databaseReference: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val TAG = "DentistMapFragment"
    private var userLocation: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dentist_map, container, false)
        val ibtnHome = view.findViewById<ImageButton>(R.id.ibtnHome)

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("dentists")

        // Initialize FusedLocationProviderClient for accessing the user location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Get the map fragment and set the callback
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dentist_maps_to_nav_book_app_client1)
        }

        // Load dentist locations on map initialization
        loadDentistLocations()

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Show the user's current location on the map
        showUserLocation()
    }

    private fun loadDentistLocations() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dentistSnapshot in snapshot.children) {
                    val name = dentistSnapshot.child("name").getValue(String::class.java)
                    val address = dentistSnapshot.child("address").getValue(String::class.java)

                    if (name != null && address != null) {
                        getCoordinatesFromAddress(address, name)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Log.e(TAG, "Database error: ${error.message}")
                Toast.makeText(requireContext(), "Failed to load dentist locations.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getCoordinatesFromAddress(address: String, name: String) {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())

        try {
            val addressList = geocoder.getFromLocationName(address, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val location = addressList[0]
                val latLng = LatLng(location.latitude, location.longitude)

                // Add a marker for the dentist location
                val marker =  mMap.addMarker(
                    MarkerOptions().position(latLng).title(name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                // Ensure the marker is always visible
                if (marker != null) {
                    marker.isVisible = true
                    marker.showInfoWindow()
                }
                // Log.d(TAG, "Marker added for $name at $latLng")
            } else {
                // Log.e(TAG, "Geocoding failed for address: $address")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Log.e(TAG, "Geocoding error: ${e.message}")
        }
    }

    private fun showUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    val marker =  mMap.addMarker(
                        MarkerOptions().position(userLocation!!).title("You Are Here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )
                    // Ensure the marker is always visible
                    if (marker != null) {
                        marker.isVisible = true
                        marker.showInfoWindow()
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation!!, 10f))
                }
            }
        } else {
            requestLocationPermissions()
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                showUserLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission is required to show your position on the map.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
