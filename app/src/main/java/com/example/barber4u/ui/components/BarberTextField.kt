package com.example.barber4u.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun BarberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) }
    )
}
