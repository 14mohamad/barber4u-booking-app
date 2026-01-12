package com.example.barber4u.model

import java.io.Serializable

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "customer" // "customer", "barber", "admin"
) : Serializable
