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
fun LessonsScreen(onNavigateBack: () -> Unit, onLessonClick: (String) -> Unit) {
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
            Text("Contenido de lecciones en construcción", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onLessonClick("lesson1") }, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = SignLearnShapes.CategoryButton) {
                Text("Ir a práctica")
            }
        }
    }
}
