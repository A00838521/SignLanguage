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
import com.signlearn.app.ui.components.ExoLoopingVideoPlayer
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.signlearn.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordOfTheDayScreen(
    onNavigateBack: () -> Unit,
    mediaTitle: String? = null,
    videoUri: android.net.Uri? = null,
    imageUri: android.net.Uri? = null,
    mediaType: String = "video" // "video" | "image"
) {
    var isVisible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )
    LaunchedEffect(Unit) { isVisible = true }

    val currentDate = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "MX")).format(Date()) }

    val wordOfDay = remember(mediaTitle) {
        val title = mediaTitle ?: "Seña del día"
        WordData(
            word = title,
            translation = "",
            description = "Explora la seña seleccionada para hoy y practica su movimiento.",
            category = "Básico",
            imageUrl = "https://images.unsplash.com/photo-1573497620053-ea5300f94f21",
            tips = listOf(
                "Observa con atención el movimiento",
                "Repite la seña varias veces",
                "Practica frente a un espejo"
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
                    // Migrado a ExoPlayer para mejor manejo de buffers y loop estable
                    when {
                        mediaType == "video" && videoUri != null -> {
                            ExoLoopingVideoPlayer(
                                uri = videoUri,
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                autoPlay = true,
                                loop = true,
                                useController = false
                            )
                        }
                        mediaType == "image" && imageUri != null -> {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = mediaTitle,
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = wordOfDay.word,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (wordOfDay.translation.isNotBlank()) {
                            Text(
                                text = wordOfDay.translation,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        AssistChip(onClick = {}, label = { Text(wordOfDay.category) })
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
