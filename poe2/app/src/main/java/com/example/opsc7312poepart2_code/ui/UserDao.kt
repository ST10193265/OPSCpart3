package com.example.opsc7312poepart2_code.ui

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User1?): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    fun getUserByUsername(username: String?): User1?

    @Query("DELETE FROM users WHERE role = 'dentists'")
    fun clearDentists()

    @Query("SELECT * FROM users WHERE role = :role")
 fun getUsersByRole(role: String = "dentists"): List<User1>
}