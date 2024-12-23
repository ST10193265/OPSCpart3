package com.example.opsc7312poepart2_code.ui

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Route to book an appointment
    @POST("api/appointments/book")
    fun bookAppointment(@Body appointment: Appointments): Call<ResponseBody>


    // Route to reschedule an appointment
    @PUT("api/appointments/reschedule/{appointmentId}")
    fun rescheduleAppointment(
        @Path("appointmentId") appointmentId: String,
        @Body appointment: Appointments
    ): Call<ResponseBody>

    // Route to cancel an appointment
    @PUT("api/appointments/cancel/{appointmentId}")
    fun cancelAppointment(
        @Path("appointmentId") appointmentId: String,
        @Body appointment: Appointments
    ): Call<ResponseBody>

     // Route to approve/confirm an appointment
    @PUT("api/appointments/approve/{appointmentId}")
    fun approveAppointment(
        @Path("appointmentId") appointmentId: String,
        @Body appointment: Appointments
    ): Call<ResponseBody>

    @GET("api/appointments/notifications/patient/{userId}")
    fun getPatientNotifications(
        @Header("Authorization") authToken: String,
        @Path("userId") userId: String,
        @Query("fcm_token") fcmToken: String
    ): Call<NotificationsResponse>

    // Route for staff notifications
    @GET("api/appointments/notifications/staff/{userId}")
    fun getDentistNotifications(
        @Header("Authorization") authToken: String,
        @Path("userId") userId: String,
        @Query("fcm_token") fcmToken: String
    ): Call<NotificationsResponse>

    // Route to get all confirmed appointments for logged-in patient
    @GET("myappointments/confirmed")
    fun getConfirmedAppointmentsForPatient(): Call<ResponseBody?>?

    // Route to get all confirmed appointments
    @GET("myappointments/allconfirmed")
    fun getConfirmedAppointments(): Call<ResponseBody?>?

    // Route to get all appointments for logged-in patient
    @GET("myappointments")
    fun getAllAppointmentsForPatient(): Call<ResponseBody?>?

    // Route to get all appointments for all patients (any status)
    @GET("allappointments")
    fun getAllAppointments(): Call<ResponseBody?>?

    // Route to register a new user (patient or staff)
    @POST("register")
    fun register(@Body user: User?): Call<ResponseBody?>?

    // Route to log in an existing user
    @POST("login")
    fun login(@Body credentials: Credentials?): Call<ResponseBody?>?

    // Route for password reset functionality
    @POST("forget-password")
    fun forgetPassword(@Body email: String?): Call<ResponseBody?>?
}


data class Appointments(
    var date: String = "",        // Date of appointment
    var dentist: String = "",
    var dentistId: String = "",   // Unique ID of the dentist
    var clientUsername: String = "",
    var userId: String = "",     // Unique ID of the client
    var description: String = "",  // Brief description of the appointment
    var slot: String = "",         // Time slot for the appointment
    var status: String = ""        // Status of the appointment
)

data class BookTimeOff(
    val dentistId: String? = "",
    val startDate: String = "",   // Start date of the time off, e.g., "dd/MM/yyyy"
    val endDate: String = "",     // End date of the time off, e.g., "dd/MM/yyyy"
    val description: String = ""  // Optional description for the time off
)


data class User(
    val id: String?, // Optional for registration
    val name: String,
    val email: String,
    val password: String,
    val role: String // e.g., "patient" or "staff"
)

data class Credentials(
    val email: String,
    val password: String
)
data class Notification(
    val appointmentId: String,
    val message: String,
    val date: String,
    val time: String,
    val description: String,
    val status: String,
    val priority: Int = 0, // Optional: default priority level
    val isRead: Boolean = false, // Optional: default read status
    val fcmToken: String? = null // Optional: FCM token for sending notifications
)

data class NotificationsResponse(
    val notifications: List<Notification>
)

