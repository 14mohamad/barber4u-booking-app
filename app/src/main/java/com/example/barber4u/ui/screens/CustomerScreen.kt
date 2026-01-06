package com.example.barber4u.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
@Composable
fun CustomerScreen() {
    Scaffold { innerPadding ->
        Text(
            "Customer Dashboard",
            modifier = Modifier.padding(innerPadding).padding(24.dp)
        )
    }
}