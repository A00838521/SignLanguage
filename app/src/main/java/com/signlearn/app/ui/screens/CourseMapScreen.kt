package com.signlearn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.signlearn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseMapScreen(onNavigateBack: () -> Unit, onLessonClick: (String) -> Unit) {
    val lessons = remember {
        listOf(
            LessonItem("Básico 1", "Introducción", true, 100),
            LessonItem("Básico 2", "Saludos", true, 60),
            LessonItem("Intermedio 1", "Familia", false, 0),
            LessonItem("Intermedio 2", "Trabajo", false, 0)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa del curso") },
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
                .padding(16.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(lessons) { lesson ->
                    Card(onClick = { if (lesson.unlocked) onLessonClick(lesson.title) }, colors = CardDefaults.cardColors(containerColor = if (lesson.unlocked) MaterialTheme.colorScheme.surface else Muted), enabled = lesson.unlocked) {
                        Column(Modifier.padding(16.dp)) {
                            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                            Text(lesson.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { lesson.progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), trackColor = Muted, color = Primary)
                        }
                    }
                }
            }
        }
    }
}

data class LessonItem(val title: String, val subtitle: String, val unlocked: Boolean, val progress: Int)
