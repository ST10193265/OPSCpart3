package com.example.opsc7312poepart2_code.ui

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User1::class, Appointments1::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun appointmentDao(): AppointmentDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `appointments` (
                `Id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `date` TEXT NOT NULL,
                `dentist` TEXT NOT NULL,
                `dentistId` TEXT,
                `clientUsername` TEXT NOT NULL,
                `userId` TEXT,
                `description` TEXT NOT NULL,
                `slot` TEXT NOT NULL,
                `status` TEXT NOT NULL
            )
        """.trimIndent())
    }
}
