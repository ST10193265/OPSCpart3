package com.example.opsc7312poepart2_code.ui

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AppointmentDao {
    @Insert
    fun insert(appointment: Appointments1)

    @Query("SELECT * FROM appointments")
    fun getAllAppointments(): List<Appointments1>

    @Delete
    fun delete(appointment: Appointments1)
}
