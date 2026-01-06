package com.example.barber4u.model

import java.io.Serializable

data class Barber(
    val id: String = "",
    val userId: String = "",       // Reference to the User account
    val branch: String = "",
    val availability: List<Availability> = emptyList() // List of available time slots
) : Serializable

data class Availability(
    val date: String = "",       // Format: "YYYY-MM-DD"
    val startTime: String = "",  // Format: "HH:MM"
    val endTime: String = ""     // Format: "HH:MM"
) : Serializable
