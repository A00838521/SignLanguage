package com.signlearn.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.signlearn.app.ui.screens.*
import com.signlearn.app.ui.theme.SignLearnShapes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.signlearn.app.ui.util.sanitizeTitle
import com.signlearn.app.ui.auth.AuthState
import com.signlearn.app.ui.auth.AuthViewModel
import com.signlearn.app.data.firebase.UserRepository
import com.signlearn.app.data.firebase.VideoRepository
import com.signlearn.app.data.model.SignVideo
import kotlinx.coroutines.launch

/**
 * SignLearnApp - Componente principal de la aplicación
 *
 * Simula un dispositivo móvil (iPhone 14 Pro: 390x844px) con:
 * - Bordes redondeados
 * - Notch simulado
 * - Navegación entre pantallas
 */
@Composable
fun SignLearnApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val userRepo = remember { UserRepository() }
    val videoRepo = remember { VideoRepository() }
    val scope = rememberCoroutineScope()

    // Navegación reactiva según estado de autenticación
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate("word_of_day") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthState.Guest -> {
                navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                }
            }
            else -> { /* Mantener login */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0),
                        Color(0xFFCBD5E1)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Contenedor del dispositivo móvil
        Box(
            modifier = Modifier
                .width(390.dp)
                .height(844.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = SignLearnShapes.PhoneContainer
                )
                .clip(SignLearnShapes.PhoneContainer)
                .background(MaterialTheme.colorScheme.background)
                .border(
                    width = 8.dp,
                    color = Color(0xFF1E293B), // slate-800
                    shape = SignLearnShapes.PhoneContainer
                )
        ) {
            // Notch del teléfono (simulando Dynamic Island / Notch)
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(28.dp)
                    .align(Alignment.TopCenter)
                    .clip(SignLearnShapes.PhoneNotch)
                    .background(Color(0xFF1E293B)) // slate-800
            )

            // Contenido de la aplicación
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        val (loading, error) = when (val st = authState) {
                            is AuthState.Loading -> true to null
                            is AuthState.Error -> false to st.message
                            else -> false to null
                        }
                        LoginScreen(
                            onLoginEmail = { email, pass -> authViewModel.signInWithEmail(email, pass) },
                            onRegister = { email, pass -> authViewModel.registerWithEmail(email, pass) },
                            onLoginGoogleIdToken = { token -> authViewModel.signInWithGoogleIdToken(token) },
                            onContinueAsGuest = { authViewModel.signInAsGuest() },
                            uiError = error,
                            uiLoading = loading
                        )
                    }
                    // Palabra del día
                    composable("word_of_day") {
                        // Selección determinista diaria: usa el día del año como semilla
                        val videosState = androidx.compose.runtime.produceState<List<SignVideo>>(initialValue = emptyList(), key1 = authState) {
                            value = runCatching { videoRepo.listVideos() }.getOrDefault(emptyList())
                        }
                        val todayIndex = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                        val selected = videosState.value.let { list ->
                            if (list.isNotEmpty()) list[todayIndex % list.size] else null
                        }
                        val videoUriState = androidx.compose.runtime.produceState<android.net.Uri?>(initialValue = null, key1 = selected?.storagePath) {
                            value = selected?.let { runCatching { videoRepo.getDownloadUrl(it.storagePath) }.getOrNull() }
                        }
                        WordOfTheDayScreen(
                            onNavigateBack = { navController.navigate("dashboard") },
                            videoTitle = sanitizeTitle(selected?.title),
                            videoUri = videoUriState.value
                        )
                    }
                    // Dashboard principal
                    composable("dashboard") {
                        // Datos de usuario
                        val dashboardData = androidx.compose.runtime.produceState(initialValue = DashboardData.guest(), key1 = authState) {
                            value = when (val st = authState) {
                                is AuthState.Authenticated -> {
                                    val uid = st.user.uid
                                    // asegurar perfil mínimo
                                    runCatching { userRepo.ensureUserProfile(uid, st.user.displayName, st.user.email) }
                                    runCatching { userRepo.updateStreak(uid) }
                                    val profile = runCatching { userRepo.getUserProfile(uid) }.getOrNull()
                                    val completed = runCatching { userRepo.getCompletedLessonsCount(uid) }.getOrDefault(0)
                                    DashboardData(
                                        userName = st.user.displayName ?: (profile?.displayName ?: "Tú"),
                                        totalPoints = profile?.totalPoints ?: 0,
                                        completedLessons = completed,
                                        totalLessons = 45,
                                        streak = profile?.streak ?: 0,
                                        dailyGoal = profile?.dailyGoal ?: 50,
                                        xpHistory = profile?.xpHistory ?: emptyList()
                                    )
                                }
                                is AuthState.Guest -> DashboardData.guest()
                                else -> DashboardData.guest()
                            }
                        }
                        DashboardScreen(
                            onNavigateToWordOfDay = { navController.navigate("word_of_day") },
                            onNavigateToCourseMap = { navController.navigate("course_map") },
                            onNavigateToPractice = { navController.navigate("practice") },
                            onNavigateToProgress = { navController.navigate("progress") },
                            onNavigateToDictionary = { navController.navigate("dictionary") },
                            onNavigateToCamera = { navController.navigate("camera") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            userName = dashboardData.value.userName,
                            totalPoints = dashboardData.value.totalPoints,
                            completedLessons = dashboardData.value.completedLessons,
                            totalLessons = dashboardData.value.totalLessons,
                            streak = dashboardData.value.streak
                        )
                    }
                    // Mapa del curso
                    composable("course_map") {
                        val uid = (authState as? AuthState.Authenticated)?.user?.uid
                        CourseMapScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onLessonClick = { lessonId ->
                                navController.navigate("lessons?lessonId=$lessonId")
                            },
                            uid = uid
                        )
                    }
                    // Lecciones (acepta optional query param `lessonId` para navegar desde mapa)
                    composable(
                        route = "lessons?lessonId={lessonId}",
                        arguments = listOf(navArgument("lessonId") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        })
                    ) { backStackEntry ->
                        val uid = (authState as? AuthState.Authenticated)?.user?.uid
                        val lessonIdArg = backStackEntry.arguments?.getString("lessonId")
                        LessonsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onLessonClick = { lessonId ->
                                navController.navigate("practice?lessonId=$lessonId")
                            },
                            uid = uid,
                            initialLessonId = lessonIdArg
                        )
                    }
                    // Práctica con ejercicios (acepta optional query param `lessonId`)
                    composable(
                        route = "practice?lessonId={lessonId}",
                        arguments = listOf(navArgument("lessonId") {
                            type = NavType.StringType
                            defaultValue = "lesson_saludos_1"
                            nullable = true
                        })
                    ) { backStackEntry ->
                        val lessonIdArg = backStackEntry.arguments?.getString("lessonId") ?: "lesson_saludos_1"
                        PracticeScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onCompleteExercise = { /* flujo ahora manejado dentro de PracticeScreen */ },
                            lessonId = lessonIdArg,
                            uid = (authState as? AuthState.Authenticated)?.user?.uid
                        )
                    }

                    // Progreso y estadísticas
                    composable("progress") {
                        val data = androidx.compose.runtime.produceState(initialValue = DashboardData.guest(), key1 = authState) {
                            value = when (val st = authState) {
                                is AuthState.Authenticated -> {
                                    val uid = st.user.uid
                                    runCatching { userRepo.updateStreak(uid) }
                                    val profile = runCatching { userRepo.getUserProfile(uid) }.getOrNull()
                                    val completed = runCatching { userRepo.getCompletedLessonsCount(uid) }.getOrDefault(0)
                                    DashboardData(
                                        userName = st.user.displayName ?: profile?.displayName ?: "Tú",
                                        totalPoints = profile?.totalPoints ?: 0,
                                        completedLessons = completed,
                                        totalLessons = 45,
                                        streak = profile?.streak ?: 0,
                                        dailyGoal = profile?.dailyGoal ?: 50,
                                        xpHistory = profile?.xpHistory ?: emptyList()
                                    )
                                }
                                is AuthState.Guest -> DashboardData.guest()
                                else -> DashboardData.guest()
                            }
                        }
                        ProgressScreen(
                            onNavigateBack = { navController.popBackStack() },
                            totalPoints = data.value.totalPoints,
                            completedLessons = data.value.completedLessons,
                            totalLessons = data.value.totalLessons,
                            streak = data.value.streak,
                            dailyGoal = data.value.dailyGoal,
                            xpHistory = data.value.xpHistory
                        )
                    }
                    // Configuración y perfil
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onLogout = {
                                authViewModel.signOut()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    // Diccionario de señas
                    composable("dictionary") {
                        DictionaryScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    // Cámara en vivo / Traductor
                    composable("camera") {
                        CameraTranslatorScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp"
)
@Composable
fun SignLearnAppPreview() {
    SignLearnApp()
}

private data class DashboardData(
    val userName: String,
    val totalPoints: Int,
    val completedLessons: Int,
    val totalLessons: Int,
    val streak: Int,
    val dailyGoal: Int,
    val xpHistory: List<Int>
) {
    companion object {
        fun guest() = DashboardData(
            userName = "Invitado",
            totalPoints = 0,
            completedLessons = 0,
            totalLessons = 45,
            streak = 0,
            dailyGoal = 50,
            xpHistory = emptyList()
        )
    }
}
