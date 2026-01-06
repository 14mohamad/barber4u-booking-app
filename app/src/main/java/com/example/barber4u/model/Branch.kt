package com.example.barber4u.model

data class Branch(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val placeId: String = "" // Google Places API ID
)