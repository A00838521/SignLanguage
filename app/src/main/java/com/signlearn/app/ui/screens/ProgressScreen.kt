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
fun ProgressScreen(onNavigateBack: () -> Unit, totalPoints: Int = 0, completedLessons: Int = 0, totalLessons: Int = 0, streak: Int = 0, dailyGoal: Int = 50, xpHistory: List<Int> = emptyList()) {
    val vm: CourseViewModel = viewModel()
    val skills by vm.skills.collectAsState()
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
            Text("Streak diario: $streak 🔥", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            if (skills.isNotEmpty()) {
                Text("Skills disponibles: ${skills.size}", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            val todayXP = xpHistory.lastOrNull() ?: 0
            Text("Meta diaria: $dailyGoal XP", style = MaterialTheme.typography.bodyMedium)
            val todayRatio = if (dailyGoal > 0) (todayXP.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(progress = { todayRatio }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Tertiary, trackColor = Muted)
            Text("Hoy: $todayXP XP de $dailyGoal", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("Historia XP (últimos ${xpHistory.size} días)", style = MaterialTheme.typography.titleSmall)
            if (xpHistory.isEmpty()) {
                Text("Sin datos todavía", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val max = (xpHistory.maxOrNull() ?: 0).coerceAtLeast(1)
                    xpHistory.forEach { v ->
                        val ratioBar = v.toFloat() / max.toFloat()
                        Box(
                            Modifier
                                .weight(1f)
                                .height(60.dp)
                                .background(Muted)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(ratioBar)
                                    .background(Tertiary.copy(alpha = 0.85f))
                            )
                        }
                    }
                }
                Text("Total reciente: ${xpHistory.sum()} XP", style = MaterialTheme.typography.bodySmall)

                // Vista detallada: fechas inferidas (hoy y días previos)
                Spacer(Modifier.height(8.dp))
                Text("Detalle por día:", style = MaterialTheme.typography.titleSmall)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
                val cal = java.util.Calendar.getInstance()
                Column(Modifier.fillMaxWidth()) {
                    xpHistory.reversed().forEachIndexed { idx, v ->
                        val dayOffset = idx
                        cal.timeInMillis = System.currentTimeMillis()
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -dayOffset)
                        val dateLabel = sdf.format(java.util.Date(cal.timeInMillis))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dateLabel, style = MaterialTheme.typography.bodySmall)
                            Text("$v XP", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
