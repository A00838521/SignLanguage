package com.androidsignfigma.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CourseMapScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mapa del Curso", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onBack) { Text("Volver") }
        Button(onClick = { onNavigate("lessons") }) { Text("Ir a Lecciones") }
    }
}
