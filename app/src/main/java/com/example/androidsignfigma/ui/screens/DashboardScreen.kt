package com.androidsignfigma.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onNavigate("course_map") }) { Text("Mapa") }
            Button(onClick = { onNavigate("lessons") }) { Text("Lecciones") }
            Button(onClick = { onNavigate("practice") }) { Text("Práctica") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onNavigate("progress") }) { Text("Progreso") }
            Button(onClick = { onNavigate("dictionary") }) { Text("Diccionario") }
            Button(onClick = { onNavigate("camera") }) { Text("Cámara") }
        }
        Button(onClick = { onNavigate("settings") }) { Text("Ajustes") }
    }
}
