package com.signlearn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signlearn.app.ui.viewmodel.CourseViewModel
import com.signlearn.app.data.firebase.UserRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.signlearn.app.ui.theme.*
import com.signlearn.app.data.firebase.VideoRepository
import com.signlearn.app.ui.components.ExoLoopingVideoPlayer
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(onNavigateBack: () -> Unit, onCompleteExercise: () -> Unit, lessonId: String = "lesson_saludos_1", uid: String? = null) {
    val vm: CourseViewModel = viewModel()
    val exercises by vm.exercises.collectAsState()
    val loading by vm.loading.collectAsState()
    var localScore by remember { mutableStateOf(0) }
    var currentIndex by remember { mutableStateOf(0) }
    val current = exercises.getOrNull(currentIndex)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var lastAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    val userRepo = remember { UserRepository() }
    val videoRepo = remember { VideoRepository() }
    LaunchedEffect(lessonId) { vm.loadExercises(lessonId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Práctica") },
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
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (current == null && !loading) {
                Text("No hay ejercicios para esta lección", style = MaterialTheme.typography.bodyMedium)
            } else if (current != null) {
                LinearProgressIndicator(progress = (currentIndex + 1f) / exercises.size.coerceAtLeast(1), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (current.videoStoragePath != null) {
                            val videoUriState = produceState<Uri?>(initialValue = null, key1 = current.videoStoragePath) {
                                value = runCatching { videoRepo.getDownloadUrl(current.videoStoragePath!!) }.getOrNull()
                            }
                            videoUriState.value?.let { uri ->
                                ExoLoopingVideoPlayer(
                                    uri = uri,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    autoPlay = true,
                                    loop = true,
                                    useController = false
                                )
                            }
                        }
                        Text(current.prompt, style = MaterialTheme.typography.titleMedium)
                        if (current.options.size >= 4) {
                            // Mostrar en grid 2x2
                            val opts = current.options
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (row in 0..1) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        for (col in 0..1) {
                                            val idx = row * 2 + col
                                            if (idx < opts.size) {
                                                val opt = opts[idx]
                                                val isSelected = selectedIndex == idx
                                                val isCorrect = lastAnswerCorrect == true && idx == current.correctIndex
                                                val container = when {
                                                    isCorrect -> Tertiary
                                                    isSelected && lastAnswerCorrect == false -> MaterialTheme.colorScheme.error
                                                    else -> Primary
                                                }
                                                Button(
                                                    onClick = {
                                                        selectedIndex = idx
                                                        val correct = idx == current.correctIndex
                                                        lastAnswerCorrect = correct
                                                        if (correct) {
                                                            localScore += current.xpReward
                                                            if (uid != null) {
                                                                scope.launch { runCatching { userRepo.addXP(uid, current.xpReward) } }
                                                            }
                                                            scope.launch {
                                                                kotlinx.coroutines.delay(700)
                                                                selectedIndex = null
                                                                lastAnswerCorrect = null
                                                                if (currentIndex < exercises.lastIndex) {
                                                                    currentIndex += 1
                                                                } else {
                                                                    onCompleteExercise()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = container),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(opt)
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Fallback list
                            current.options.forEachIndexed { idx, opt ->
                                val isSelected = selectedIndex == idx
                                val isCorrect = lastAnswerCorrect == true && idx == current.correctIndex
                                val container = when {
                                    isCorrect -> Tertiary
                                    isSelected && lastAnswerCorrect == false -> MaterialTheme.colorScheme.error
                                    else -> Primary
                                }
                                Button(
                                    onClick = {
                                        selectedIndex = idx
                                        val correct = idx == current.correctIndex
                                        lastAnswerCorrect = correct
                                        if (correct) {
                                            localScore += current.xpReward
                                            if (uid != null) {
                                                scope.launch { runCatching { userRepo.addXP(uid, current.xpReward) } }
                                            }
                                            scope.launch {
                                                kotlinx.coroutines.delay(700)
                                                selectedIndex = null
                                                lastAnswerCorrect = null
                                                if (currentIndex < exercises.lastIndex) {
                                                    currentIndex += 1
                                                } else {
                                                    onCompleteExercise()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = container),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(opt)
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                        // Feedback textual breve
                        lastAnswerCorrect?.let { ok ->
                            if (ok) Text("Correcto ✅", style = MaterialTheme.typography.bodySmall, color = Tertiary)
                            else Text("Incorrecto — intenta otra vez", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Ejercicio ${currentIndex + 1} de ${exercises.size}", style = MaterialTheme.typography.bodySmall)
                Text("XP sesión: $localScore", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
