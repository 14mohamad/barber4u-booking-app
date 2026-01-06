package com.example.barber4u.model

// Appointment.kt
data class Appointment(
    val id: String = "",
    val customerId: String = "",
    val barberId: String = "",
    val branch: String = "",
    val date: String = "",
    val time: String = "",
    val status: String = "confirmed"
)
