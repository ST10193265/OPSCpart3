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

interface ApiService {

    // Route to book an appointment
    @POST("book")
    fun bookAppointment(@Body appointment: Appointment?): Call<ResponseBody?>?

    // Route to reschedule an appointment
    @PUT("{appointmentId}")
    fun rescheduleAppointment(
        @Body appointment: Appointment?,
        @Path("appointmentId") appointmentId: String?
    ): Call<ResponseBody?>?

    // Route to cancel an appointment
    @DELETE("{appointmentId}")
    fun cancelAppointment(@Path("appointmentId") appointmentId: String?): Call<ResponseBody?>?

    // Route to approve/confirm an appointment
    @PUT("{appointmentId}/approve")
    fun approveAppointment(@Path("appointmentId") appointmentId: String?): Call<ResponseBody?>?

    // Route for patient notifications
    @GET("api/appointments/notifications/patient")
    fun getPatientNotifications(

    ): Call<NotificationsResponse>

    // Route for staff notifications
    @GET("notifications/staff")
    fun getStaffNotifications(): Call<ResponseBody?>?

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

data class Appointment(
    val id: String?, // Optional for booking; should be provided for rescheduling
    val patientId: String,
    val doctorId: String,
    val dateTime: String, // Consider using a Date type for better handling
    val notes: String?
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
    val status: String
)

data class NotificationsResponse(
    val notifications: List<Notification>
)


