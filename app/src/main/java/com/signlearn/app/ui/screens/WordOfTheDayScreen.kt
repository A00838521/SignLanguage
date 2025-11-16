package com.signlearn.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.signlearn.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordOfTheDayScreen(onNavigateBack: () -> Unit, videoTitle: String? = null, videoUri: android.net.Uri? = null) {
    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )
    LaunchedEffect(Unit) { isVisible = true }

    val currentDate = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "MX")).format(Date()) }

    val wordOfDay = remember {
        WordData(
            word = "Gracias",
            translation = "Thank you",
            description = "Gesto fundamental de cortesía. Se realiza colocando la mano abierta cerca del pecho y moviendo hacia adelante con un gesto suave.",
            category = "Básico",
            imageUrl = "https://images.unsplash.com/photo-1573497620053-ea5300f94f21",
            tips = listOf(
                "Mantén la mano abierta y relajada",
                "El movimiento debe ser suave y natural",
                "Puedes acompañar con una sonrisa"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Palabra del día") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = OnPrimary, navigationIconContentColor = OnPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(brush = Brush.verticalGradient(colors = listOf(Primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = currentDate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(modifier = Modifier.fillMaxWidth().scale(scale), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), shape = SignLearnShapes.CardElevated) {
                Column {
                    AsyncImage(
                        model = wordOfDay.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = wordOfDay.word, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = wordOfDay.translation, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AssistChip(onClick = {}, label = { Text(wordOfDay.category) })
                    }
                }
            }
            if (videoUri != null) {
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = videoTitle ?: "Video", style = MaterialTheme.typography.titleMedium)
                        com.signlearn.app.ui.components.VideoPlayer(uri = videoUri, modifier = Modifier.fillMaxWidth().height(220.dp))
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Descripción", style = MaterialTheme.typography.titleMedium)
                    Text(text = wordOfDay.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Consejos", style = MaterialTheme.typography.titleMedium)
                    wordOfDay.tips.forEach { tip -> Text("• $tip", style = MaterialTheme.typography.bodyMedium) }
                }
            }
            Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = SignLearnShapes.CategoryButton) {
                Text("Volver al dashboard")
            }
        }
    }
}

data class WordData(
    val word: String,
    val translation: String,
    val description: String,
    val category: String,
    val imageUrl: String,
    val tips: List<String>
)
