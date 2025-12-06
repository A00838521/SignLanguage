package com.signlearn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signlearn.app.ui.viewmodel.CourseViewModel
import com.signlearn.app.data.firebase.UserRepository
import com.signlearn.app.data.model.UserPracticeState
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.signlearn.app.ui.theme.*
import com.signlearn.app.data.firebase.VideoRepository
import com.signlearn.app.data.firebase.ImageRepository
import com.signlearn.app.data.firebase.LearningRepository
import com.signlearn.app.ui.components.ExoLoopingVideoPlayer
import android.net.Uri
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(onNavigateBack: () -> Unit, onCompleteExercise: () -> Unit, lessonId: String = "lesson_saludos_1", uid: String? = null, isOffline: Boolean = false) {
    val vm: CourseViewModel = viewModel()
    val exercises by vm.exercises.collectAsState()
    val loading by vm.loading.collectAsState()
    var localScore by remember { mutableStateOf(0) }
    var currentIndex by remember { mutableStateOf(0) }
    var activeLessonId by remember { mutableStateOf(lessonId) }
    val current = exercises.getOrNull(currentIndex)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var lastAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var inputLocked by remember { mutableStateOf(false) }
    // Estado de transición entre lecciones/categorías para mantener "Continuar" visible
    var isAdvancing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val userRepo = remember { UserRepository() }
    val learningRepo = remember { LearningRepository() }
    val videoRepo = remember { VideoRepository() }
    val imageRepo = remember { ImageRepository() }
    val urlCache = remember { mutableStateMapOf<String, Uri?>() }
    LaunchedEffect(lessonId, uid) {
        if (uid != null && (lessonId.isBlank() || lessonId == "auto")) {
            // Recuperar estado previo
            val state = runCatching { userRepo.getPracticeState(uid) }.getOrNull()
            if (state != null && state.lessonId.isNotBlank()) {
                Log.d("PracticeFlow", "Restore practice state: lessonId=${state.lessonId} idx=${state.exerciseIndex}")
                vm.loadExercises(state.lessonId)
                currentIndex = state.exerciseIndex.coerceAtLeast(0)
            } else {
                // Fallback: primera lección desbloqueada (primer unit -> primer skill -> primer lesson)
                val units = runCatching { learningRepo.listUnits() }.getOrDefault(emptyList())
                val firstUnit = units.sortedBy { it.order }.firstOrNull()
                var resolvedLessonId = "lesson_saludos_1"
                if (firstUnit != null) {
                    val skills = runCatching { learningRepo.listSkillsForUnit(firstUnit.id) }.getOrDefault(emptyList())
                    val firstSkill = skills.sortedBy { it.order }.firstOrNull()
                    if (firstSkill != null) {
                        val lessons = runCatching { learningRepo.listLessonsForSkill(firstSkill.id) }.getOrDefault(emptyList())
                        val firstLesson = lessons.sortedBy { it.order }.firstOrNull()
                        if (firstLesson != null) {
                            resolvedLessonId = firstLesson.id
                        }
                    }
                }
                activeLessonId = resolvedLessonId
                Log.d("PracticeFlow", "Fallback first lesson resolved: ${resolvedLessonId}")
                vm.loadExercises(resolvedLessonId)
            }
        } else {
            activeLessonId = lessonId
            Log.d("PracticeFlow", "Direct lesson load: ${lessonId}")
            vm.loadExercises(lessonId)
        }
    }

    // Generación dinámica de ejercicios por usuario si no existen (auto)
    var generatedUserContent by remember { mutableStateOf(false) }
    LaunchedEffect(uid, exercises) {
        if (!generatedUserContent && uid != null && exercises.isEmpty()) {
            // Escoger primera categoría siguiendo orden pedagógico definido en LearningRepository
            val chosenCategory = runCatching { learningRepo.pickFirstOrderedCategory() }.getOrNull() ?: "abecedario"
            runCatching { learningRepo.ensureUserSkillContent(uid, chosenCategory) }
            val skillId = "user_skill_${uid}_${chosenCategory}"
            val lessons = runCatching { learningRepo.listLessonsForSkill(skillId) }.getOrDefault(emptyList())
            val firstLesson = lessons.sortedBy { it.order }.firstOrNull()
            if (firstLesson != null) {
                vm.loadExercises(firstLesson.id)
                generatedUserContent = true
            }
        }
    }

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
            // Indicador visual de transición
            if (isAdvancing) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = SignLearnShapes.CategoryButton
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Text("Cargando siguiente lección…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
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
                        // Mostrar medio (video o imagen) según mediaType
                        val mediaPath = current.mediaStoragePath ?: current.videoStoragePath
                        if (mediaPath != null && !isAdvancing) {
                            val mediaUriState = produceState<Uri?>(initialValue = null, key1 = mediaPath) {
                                value = urlCache[mediaPath] ?: run {
                                    val uri = runCatching {
                                        if (current.mediaType == "image") {
                                            imageRepo.getDownloadUrl(mediaPath)
                                        } else {
                                            videoRepo.getDownloadUrl(mediaPath)
                                        }
                                    }.getOrNull()
                                    if (uri != null) urlCache[mediaPath] = uri
                                    uri
                                }
                            }
                            mediaUriState.value?.let { uri ->
                                if (current.mediaType == "image") {
                                    coil.compose.AsyncImage(
                                        model = uri,
                                        contentDescription = current.prompt,
                                        modifier = Modifier.fillMaxWidth().height(200.dp),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                    )
                                } else {
                                    ExoLoopingVideoPlayer(
                                        uri = uri,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        autoPlay = !inputLocked && !isAdvancing,
                                        active = !isAdvancing,
                                        loop = true,
                                        useController = false
                                    )
                                }
                            }
                        }
                        Text(current.prompt, style = MaterialTheme.typography.titleMedium)
                        // Componente reutilizable para opciones con animación de error
                        @Composable
                        fun OptionGrid(options: List<String>) {
                            val opts = options
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (row in 0..1) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        for (col in 0..1) {
                                            val idx = row * 2 + col
                                            if (idx < opts.size) {
                                                AnimatedOptionButton(
                                                    text = opts[idx],
                                                    selected = selectedIndex == idx,
                                                    correct = lastAnswerCorrect == true && idx == current.correctIndex,
                                                    incorrectSelected = selectedIndex == idx && lastAnswerCorrect == false,
                                                    onClick = {
                                                        if (inputLocked) return@AnimatedOptionButton
                                                        selectedIndex = idx
                                                        val correct = idx == current.correctIndex
                                                        lastAnswerCorrect = correct
                                                        if (correct) {
                                                            inputLocked = true
                                                            localScore += current.xpReward
                                                            if (uid != null && !isOffline) {
                                                                scope.launch { runCatching { userRepo.addXP(uid, current.xpReward) } }
                                                            }
                                                            scope.launch {
                                                                kotlinx.coroutines.delay(700)
                                                                selectedIndex = null
                                                                lastAnswerCorrect = null
                                                                if (currentIndex < exercises.lastIndex) {
                                                                    currentIndex += 1
                                                                    Log.d("PracticeFlow", "Advance within lesson: lesson=${activeLessonId} nextIndex=${currentIndex}")
                                                                    if (uid != null) {
                                                                        runCatching { if (!isOffline) userRepo.setPracticeState(uid, activeLessonId, currentIndex) }
                                                                    }
                                                                    inputLocked = false
                                                                } else {
                                                                    // Final de lección: marcar completada y regresar al mapa de curso
                                                                    isAdvancing = true
                                                                    Log.d("PracticeFlow", "End of lesson reached; isAdvancing=true, inputLocked=${'$'}inputLocked")
                                                                    if (uid != null) {
                                                                        runCatching { if (!isOffline) userRepo.markLessonCompleted(uid, activeLessonId, localScore) }
                                                                        runCatching { if (!isOffline) userRepo.setPracticeState(uid, activeLessonId, 0) }
                                                                        // Desbloquear categorías justo al completar
                                                                        runCatching { if (!isOffline) vm.refreshCategories(uid) }
                                                                    }
                                                                    isAdvancing = false
                                                                    onCompleteExercise()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                        @Composable
                        fun OptionList(options: List<String>) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                options.forEachIndexed { idx, opt ->
                                    AnimatedOptionButton(
                                        text = opt,
                                        selected = selectedIndex == idx,
                                        correct = lastAnswerCorrect == true && idx == current.correctIndex,
                                        incorrectSelected = selectedIndex == idx && lastAnswerCorrect == false,
                                        onClick = {
                                            if (inputLocked) return@AnimatedOptionButton
                                            selectedIndex = idx
                                            val correct = idx == current.correctIndex
                                            lastAnswerCorrect = correct
                                            if (correct) {
                                                inputLocked = true
                                                localScore += current.xpReward
                                                if (uid != null && !isOffline) {
                                                    scope.launch { runCatching { userRepo.addXP(uid, current.xpReward) } }
                                                }
                                                scope.launch {
                                                    kotlinx.coroutines.delay(700)
                                                    selectedIndex = null
                                                    lastAnswerCorrect = null
                                                    if (currentIndex < exercises.lastIndex) {
                                                        currentIndex += 1
                                                        Log.d("PracticeFlow", "Advance within lesson (list): lesson=${activeLessonId} nextIndex=${currentIndex}")
                                                        if (uid != null) {
                                                            runCatching { if (!isOffline) userRepo.setPracticeState(uid, activeLessonId, currentIndex) }
                                                        }
                                                        inputLocked = false
                                                    } else {
                                                        // Final de lección: marcar completada y regresar al mapa
                                                        isAdvancing = true
                                                        Log.d("PracticeFlow", "End of lesson reached (list); isAdvancing=true, inputLocked=${'$'}inputLocked")
                                                        if (uid != null) {
                                                            runCatching { if (!isOffline) userRepo.markLessonCompleted(uid, activeLessonId, localScore) }
                                                            runCatching { if (!isOffline) userRepo.setPracticeState(uid, activeLessonId, 0) }
                                                            runCatching { if (!isOffline) vm.refreshCategories(uid) }
                                                        }
                                                        isAdvancing = false
                                                        onCompleteExercise()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        if (current.options.size >= 4) OptionGrid(current.options) else OptionList(current.options)
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
                // Se elimina el botón "Continuar"; la navegación ocurre automáticamente al finalizar
            }
        }
    }

}

// Calcula el siguiente lessonId por convención:
// - Global:  lesson_{slug}_{N} -> lesson_{slug}_{N+1}
// - Usuario: user_{skillId}_lesson_{N} -> user_{skillId}_lesson_{N+1}
private fun computeNextLessonId(current: String): String? {
    return if (current.startsWith("user_")) {
        val after = current.removePrefix("user_")
        val parts = after.split("_lesson_")
        if (parts.size == 2) {
            val skillId = parts[0]
            val n = parts[1].toIntOrNull() ?: return null
            "user_${skillId}_lesson_${n + 1}"
        } else null
    } else if (current.startsWith("lesson_")) {
        val lastUnderscore = current.lastIndexOf('_')
        if (lastUnderscore > 0 && lastUnderscore < current.length - 1) {
            val prefix = current.substring(0, lastUnderscore)
            val num = current.substring(lastUnderscore + 1).toIntOrNull() ?: return null
            "${prefix}_${num + 1}"
        } else null
    } else null
}

// Calcula la primera lección de la siguiente categoría según el orden pedagógico.
// Si es contenido de usuario, asegura la generación de contenido y devuelve lesson_1 de la nueva categoría.
private suspend fun computeNextCategoryFirstLesson(currentLessonId: String, uid: String?, repo: LearningRepository): String? {
    val currentSlug: String? = if (currentLessonId.startsWith("lesson_")) {
        val body = currentLessonId.removePrefix("lesson_")
        if (body.contains('_')) body.substring(0, body.lastIndexOf('_')) else null
    } else if (currentLessonId.startsWith("user_")) {
        if (uid == null) return null
        val after = currentLessonId.removePrefix("user_")
        val skillId = after.substringBefore("_lesson_")
        val prefix = "user_skill_${'$'}uid" + "_"
        if (!skillId.startsWith(prefix)) return null
        skillId.removePrefix(prefix)
    } else null
    currentSlug ?: return null

    val order = LearningRepository.LEARNING_CATEGORY_ORDER
    val idx = order.indexOf(currentSlug)
    val nextSlug = if (idx >= 0 && idx < order.lastIndex) order[idx + 1] else null
    nextSlug ?: return null

    return if (currentLessonId.startsWith("user_")) {
        if (uid == null) return null
        runCatching { repo.ensureUserSkillContent(uid, nextSlug) }
        "user_skill_${uid}_${nextSlug}_lesson_1"
    } else {
        "lesson_${nextSlug}_1"
    }
}

@Composable
private fun AnimatedOptionButton(
    text: String,
    selected: Boolean,
    correct: Boolean,
    incorrectSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = Primary
    val targetColor = when {
        correct -> Tertiary
        incorrectSelected -> MaterialTheme.colorScheme.error
        selected -> baseColor.copy(alpha = 0.7f)
        else -> baseColor
    }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "color"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeOffset by if (incorrectSelected) {
        infiniteTransition.animateFloat(
            initialValue = -6f,
            targetValue = 6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 80, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "offset"
        )
    } else remember { mutableStateOf(0f) }
    val elevation = if (selected) 6.dp else 2.dp
    Surface(
        color = animatedColor,
        shape = SignLearnShapes.CategoryButton,
        tonalElevation = elevation,
        modifier = modifier
            .height(72.dp)
            .offset(x = shakeOffset.dp)
            .clickable { onClick() }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = OnPrimary)
        }
    }
}
