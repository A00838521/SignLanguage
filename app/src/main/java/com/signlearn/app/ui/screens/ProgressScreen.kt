package com.signlearn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.signlearn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onNavigateBack: () -> Unit, totalPoints: Int = 0, completedLessons: Int = 0, totalLessons: Int = 0) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progreso") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = OnPrimary, navigationIconContentColor = OnPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(brush = Brush.verticalGradient(listOf(Primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Puntos totales: $totalPoints", style = MaterialTheme.typography.titleMedium)
            val ratio = if (totalLessons > 0) completedLessons.toFloat() / totalLessons else 0f
            LinearProgressIndicator(progress = { ratio.coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Primary, trackColor = Muted)
            Text("Lecciones completadas: $completedLessons de $totalLessons", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
