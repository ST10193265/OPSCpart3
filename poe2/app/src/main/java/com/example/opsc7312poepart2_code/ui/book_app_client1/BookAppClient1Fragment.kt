package com.example.opsc7312poepart2_code.ui.book_app_client1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.google.firebase.database.*

class BookAppClient1Fragment : Fragment() {

    private lateinit var dentistList: ArrayList<String> // List to store dentist names
    private lateinit var listViewAdapter: ArrayAdapter<String> // Adapter for the ListView
    private lateinit var databaseReference: DatabaseReference // Firebase Database reference

    // Log tag for debugging
    private val TAG = "BookAppClient1Fragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_book_app_client1, container, false)

        // Initialize the ListView
        val listView = view.findViewById<ListView>(R.id.listofDentists)

        // Initialize ImageButtons
        val ibtnMaps = view.findViewById<ImageButton>(R.id.ibtnMaps)
        val ibtnHome = view.findViewById<ImageButton>(R.id.ibtnHome)

        // Initialize the dentist list and adapter
        dentistList = ArrayList()
        listViewAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, dentistList)
        listView.adapter = listViewAdapter

        // Set up Firebase database reference to dentists
        databaseReference = FirebaseDatabase.getInstance().getReference("dentists")

        // Fetch dentists from Firebase and populate the ListView
        fetchDentists()

        // Handle ListView item click: navigate to BookAppClient2Fragment with selected dentist
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedDentist = listViewAdapter.getItem(position)  // Get the dentist from the list
            Log.d(TAG, "Selected Dentist: $selectedDentist")  // Log the selected dentist
            if (selectedDentist != null) {
                try {
                    // Create a Bundle to pass the selected dentist's name to the next fragment
                    val bundle = Bundle().apply {
                        putString("selectedDentist", selectedDentist)
                    }
                    // Navigate to BookAppClient2Fragment and pass the selected dentist's name
                    findNavController().navigate(R.id.action_nav_book_app_client1_to_nav_book_app_client2, bundle)

                } catch (e: Exception) {
                    Log.e(TAG, "Navigation error: ${e.message}")  // Log any navigation errors
                    Toast.makeText(requireContext(), "Error navigating: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Selected dentist is null")  // Log if the dentist is null
                Toast.makeText(requireContext(), "Invalid dentist selection.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listeners for the buttons
        ibtnMaps.setOnClickListener {
            Toast.makeText(requireContext(), "Maps functionality to be implemented.", Toast.LENGTH_SHORT).show()
        }

        ibtnHome.setOnClickListener {
            // Navigate back to the client menu
            findNavController().navigate(R.id.action_nav_book_app_client1_to_nav_menu_client)
        }

        return view
    }

    // Fetch dentists from Firebase Realtime Database
    private fun fetchDentists() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dentistList.clear()  // Clear the list before adding new items
                for (dentistSnapshot in snapshot.children) {
                    val dentistName = dentistSnapshot.child("name").getValue(String::class.java)
                    dentistName?.let {
                        dentistList.add(it)  // Add the dentist to the list
                        Log.d(TAG, "Fetched Dentist: $it") // Log each dentist fetched
                    }
                }
                listViewAdapter.notifyDataSetChanged()  // Notify the adapter that the data has changed
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")  // Log any database errors
                Toast.makeText(requireContext(), "Failed to load dentists.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
