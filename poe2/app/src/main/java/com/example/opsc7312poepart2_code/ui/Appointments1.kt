package com.example.opsc7312poepart2_code.ui

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointments1(
    @PrimaryKey(autoGenerate = true) val Id: Long = 0, // Set a default value here
    val date: String,
    var dentist: String = "",
    var dentistId: String = "",
    val clientUsername: String,
    var userId: String = "",     // Unique ID of the client
    var description: String = "",
    val slot: String,
    val status: String = "pending"
)

