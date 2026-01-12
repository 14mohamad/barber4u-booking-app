package com.example.barber4u.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.barber4u.model.Branch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchSelectionScreen(
    branches: List<Branch>,
    onBranchSelected: (Branch) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Select a Branch") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            items(branches) { branch ->
                BranchItem(branch = branch, onClick = { onBranchSelected(branch) })
            }
        }
    }
}

@Composable
fun BranchItem(branch: Branch, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = branch.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = branch.address, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
