package com.example.opsc7312poepart2_code.ui

import androidx.room.Entity
import androidx.room.PrimaryKey

// Data class for User1 entity
@Entity(tableName = "users")
data class User1(
    @PrimaryKey val userId: String,
    val username: String,
    val password: String,
    val role: String

)


