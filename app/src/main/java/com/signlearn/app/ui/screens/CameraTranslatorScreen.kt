package com.signlearn.app.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.signlearn.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CameraTranslatorScreen(onNavigateBack: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var isDetecting by remember { mutableStateOf(false) }
    var detectedSign by remember { mutableStateOf<DetectedSign?>(null) }
    var confidence by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Traductor en vivo") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = OnPrimary, navigationIconContentColor = OnPrimary)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                !cameraPermissionState.status.isGranted -> {
                    CameraPermissionRequest(onRequestPermission = { cameraPermissionState.launchPermissionRequest() })
                }
                else -> {
                    CameraTranslatorContent(
                        isDetecting = isDetecting,
                        detectedSign = detectedSign,
                        confidence = confidence,
                        onToggleDetection = { isDetecting = !isDetecting }
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(80.dp), tint = Primary)
        Spacer(Modifier.height(24.dp))
        Text(text = "Permiso de cámara requerido", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(text = "SignLearn necesita acceso a la cámara para poder detectar y traducir señas en tiempo real.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = SignLearnShapes.CategoryButton) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Permitir acceso")
        }
    }
}

@Composable
fun CameraTranslatorContent(isDetecting: Boolean, detectedSign: DetectedSign?, confidence: Float, onToggleDetection: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Text(text = "Vista de cámara", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "Integración de CameraX + ML Kit pendiente", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            }
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (detectedSign != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Primary), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = detectedSign.word, style = MaterialTheme.typography.displaySmall, color = OnPrimary)
                        Spacer(Modifier.height(8.dp))
                        Text(text = detectedSign.translation, style = MaterialTheme.typography.titleMedium, color = OnPrimary.copy(alpha = 0.9f))
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(progress = { confidence }, modifier = Modifier.weight(1f).height(6.dp), color = OnPrimary, trackColor = OnPrimary.copy(alpha = 0.3f))
                            Text(text = "${(confidence * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = OnPrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(12.dp), shape = androidx.compose.foundation.shape.CircleShape, color = if (isDetecting) Tertiary else MaterialTheme.colorScheme.onSurfaceVariant) {}
                            Text(text = if (isDetecting) "Detectando..." else "En pausa", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = onToggleDetection, colors = IconButtonDefaults.iconButtonColors(containerColor = if (isDetecting) Error else Primary)) {
                            Icon(imageVector = if (isDetecting) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isDetecting) "Pausar" else "Iniciar", tint = OnPrimary)
                        }
                    }
                    Text(text = "Coloca tu mano frente a la cámara y realiza una seña. El sistema la detectará y traducirá automáticamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

data class DetectedSign(val word: String, val translation: String, val category: String)
