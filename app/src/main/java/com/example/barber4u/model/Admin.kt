package com.example.barber4u.model

import java.io.Serializable

data class Admin(
    val id: String = "",
    val userId: String = "",     // Reference to the User account
    val branch: String = ""      // Admin may manage a specific branch
) : Serializable
