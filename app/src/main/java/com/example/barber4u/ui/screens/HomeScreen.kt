package com.example.barber4u.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { onNavigate("customer") }) {
                Text("Customer Screen")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onNavigate("barber") }) {
                Text("Barber Screen")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onNavigate("admin") }) {
                Text("Admin Screen")
            }
        }
    }
}
