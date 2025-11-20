package com.signlearn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun CourseMapScreen(onNavigateBack: () -> Unit, onLessonClick: (String) -> Unit, uid: String? = null) {
    val vm: CourseViewModel = viewModel()
    val units by vm.units.collectAsState()
    val skills by vm.skills.collectAsState()
    val lessons by vm.lessons.collectAsState()
    val unlockedSkills by vm.unlockedSkills.collectAsState()
    val unlockedLessons by vm.unlockedLessons.collectAsState()
    val categories by vm.categories.collectAsState()
    val unlockedCategories by vm.unlockedCategories.collectAsState()
    val loading by vm.loading.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedUnit by remember { mutableStateOf<String?>(null) }
    var selectedSkill by remember { mutableStateOf<String?>(null) }
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
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
            }
            // Categorías (derivadas dinámicamente de la colección `videos`)
            Text("Categorías", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 220.dp)) {
                items(categories) { c ->
                    val unlocked = unlockedCategories.contains(c.slug)
                    val cardColors = if (unlocked) CardDefaults.elevatedCardColors() else CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ElevatedCard(onClick = {
                        if (unlocked) {
                            selectedCategory = c.slug
                        }
                    }, colors = cardColors) {
                        Column(Modifier.padding(12.dp)) {
                            Text(c.title, style = MaterialTheme.typography.titleMedium)
                            Text("${c.count} ejercicios disponibles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!unlocked) Text("Bloqueado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Efecto: cuando se selecciona una categoría, generar ejercicios si hace falta y navegar
            if (selectedCategory != null && uid != null) {
                LaunchedEffect(selectedCategory) {
                    val skillId = vm.ensureUserExercisesForCategory(uid, selectedCategory!!)
                    val lessonId = "user_${skillId}_lesson_1"
                    onLessonClick(lessonId)
                    selectedCategory = null
                }
            } else if (selectedCategory != null && uid == null) {
                // usuario no autenticado: navegar a practice genérico
                LaunchedEffect(selectedCategory) {
                    onLessonClick("lesson_${selectedCategory}_1")
                    selectedCategory = null
                }
            }
            Spacer(Modifier.height(16.dp))
            if (selectedUnit != null) {
                Text("Skills", style = MaterialTheme.typography.titleMedium)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 200.dp)) {
                    items(skills) { s ->
                        val unlocked = unlockedSkills.contains(s.id)
                        val cardColors = if (unlocked) CardDefaults.cardColors() else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        Card(onClick = { if (unlocked) { selectedSkill = s.id; vm.loadLessons(s.id, uid) } }, colors = cardColors) {
                            Column(Modifier.padding(12.dp)) {
                                Text(s.title, style = MaterialTheme.typography.bodyLarge)
                                if (!unlocked) Text("Bloqueado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (selectedSkill != null) {
                Text("Lecciones", style = MaterialTheme.typography.titleMedium)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(lessons) { les ->
                        val unlocked = unlockedLessons.contains(les.id)
                        val cardColors = if (unlocked) CardDefaults.cardColors() else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        Card(onClick = { if (unlocked) onLessonClick(les.id) }, colors = cardColors) {
                            Column(Modifier.padding(12.dp)) {
                                Text(les.title, style = MaterialTheme.typography.bodyLarge)
                                Text("~${les.estimatedMinutes} min", style = MaterialTheme.typography.labelSmall)
                                if (!unlocked) Text("Bloqueado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Obsoleto: mantenido si alguna referencia externa existiera
data class LessonItem(val title: String, val subtitle: String, val unlocked: Boolean, val progress: Int)
