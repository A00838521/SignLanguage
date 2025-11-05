package com.signlearn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.signlearn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToWordOfDay: () -> Unit,
    onNavigateToCourseMap: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToDictionary: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val userName = "Usuario"
    val currentStreak = 7
    val totalPoints = 1250
    val completedLessons = 15
    val totalLessons = 45
    val progressPercentage = (completedLessons.toFloat() / totalLessons * 100).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SignLearn") },
                actions = { IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Configuración") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    actionIconContentColor = OnPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "¡Hola, $userName!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Continúa tu aprendizaje",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    icon = Icons.Default.Whatshot,
                    value = "$currentStreak días",
                    label = "Racha actual",
                    color = TertiaryLight,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    icon = Icons.Default.Stars,
                    value = "$totalPoints",
                    label = "Puntos totales",
                    color = PrimaryLight,
                    modifier = Modifier.weight(1f)
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Progreso del curso", style = MaterialTheme.typography.titleMedium)
                        Text(text = "$progressPercentage%", style = MaterialTheme.typography.titleLarge, color = Primary)
                    }
                    LinearProgressIndicator(
                        progress = { progressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(SignLearnShapes.Pill),
                        color = Primary,
                        trackColor = Muted
                    )
                    Text(
                        text = "$completedLessons de $totalLessons lecciones completadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToWordOfDay),
                colors = CardDefaults.cardColors(containerColor = Tertiary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Tertiary, shape = SignLearnShapes.Pill, modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = OnTertiary) }
                        }
                        Column {
                            Text(text = "Palabra del día", style = MaterialTheme.typography.titleMedium, color = Tertiary)
                            Text(text = "Descubre una nueva seña", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Ir", tint = Tertiary)
                }
            }
            Text(text = "Accesos rápidos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAccessCard(icon = Icons.Default.Map, title = "Mapa del curso", onClick = onNavigateToCourseMap, modifier = Modifier.weight(1f))
                QuickAccessCard(icon = Icons.Default.Quiz, title = "Practicar", onClick = onNavigateToPractice, modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAccessCard(icon = Icons.Default.Book, title = "Diccionario", onClick = onNavigateToDictionary, modifier = Modifier.weight(1f))
                QuickAccessCard(icon = Icons.Default.CameraAlt, title = "Traductor", onClick = onNavigateToCamera, modifier = Modifier.weight(1f))
            }
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Esta semana", style = MaterialTheme.typography.titleMedium)
                    WeeklyStatRow(icon = Icons.Default.School, label = "Lecciones completadas", value = "5")
                    WeeklyStatRow(icon = Icons.Default.Timer, label = "Tiempo de práctica", value = "2h 30m")
                    WeeklyStatRow(icon = Icons.Default.CheckCircle, label = "Precisión", value = "92%")
                    Button(onClick = onNavigateToProgress, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = SignLearnShapes.CategoryButton) {
                        Text("Ver estadísticas completas")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatsCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuickAccessCard(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.clickable(onClick = onClick), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = Primary.copy(alpha = 0.1f), shape = SignLearnShapes.Pill, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp)) }
            }
            Text(text = title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun WeeklyStatRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = Primary)
    }
}
