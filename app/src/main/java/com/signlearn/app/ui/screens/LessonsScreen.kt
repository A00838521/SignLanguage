package com.signlearn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signlearn.app.ui.viewmodel.CourseViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.signlearn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(onNavigateBack: () -> Unit, onLessonClick: (String) -> Unit, uid: String? = null, initialLessonId: String? = null) {
    val vm: CourseViewModel = viewModel()
    val lessons by vm.lessons.collectAsState()
    val unlocked by vm.unlockedLessons.collectAsState()
    val loading by vm.loading.collectAsState()

    LaunchedEffect(initialLessonId, uid) {
        initialLessonId?.takeIf { it.isNotBlank() }?.let { lid ->
            if (lid.startsWith("user_")) {
                val after = lid.removePrefix("user_")
                val skillId = after.substringBefore("_lesson_")
                vm.loadLessons(skillId, uid)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lecciones") },
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
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            Text("Lecciones cargadas: ${lessons.size}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            lessons.forEach { l ->
                val isUnlocked = unlocked.contains(l.id)
                val cardColors = if (isUnlocked) CardDefaults.cardColors() else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                Card(onClick = { if (isUnlocked) onLessonClick(l.id) }, colors = cardColors) {
                    Column(Modifier.padding(12.dp)) {
                        Text(l.title, style = MaterialTheme.typography.titleMedium)
                        Text("~${l.estimatedMinutes} min", style = MaterialTheme.typography.labelSmall)
                        if (!isUnlocked) Text("Bloqueado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
