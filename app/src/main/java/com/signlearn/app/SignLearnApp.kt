package com.signlearn.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
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
import com.signlearn.app.ui.screens.*
import com.signlearn.app.ui.theme.SignLearnShapes

/**
 * SignLearnApp - Componente principal de la aplicación
 *
 * Simula un dispositivo móvil (iPhone 14 Pro: 390x844px)
 */
@Composable
fun SignLearnApp() {
    // Estado de navegación
    val navController = rememberNavController()

    // Estado de autenticación
    var isAuthenticated by remember { mutableStateOf(false) }

    // Fondo gris con gradiente
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
        // Contenedor estilo teléfono
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
                    color = Color(0xFF1E293B),
                    shape = SignLearnShapes.PhoneContainer
                )
        ) {
            // Notch
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(28.dp)
                    .align(Alignment.TopCenter)
                    .clip(SignLearnShapes.PhoneNotch)
                    .background(Color(0xFF1E293B))
            )

            // Contenido de la aplicación
            Box(modifier = Modifier.fillMaxSize()) {

                NavHost(
                    navController = navController,
                    startDestination = if (isAuthenticated) "word_of_day" else "login"
                ) {

                    // 1. Login
                    composable("login") {
                        LoginScreen(
                            onLogin = {
                                isAuthenticated = true
                                navController.navigate("word_of_day") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 2. Palabra del día
                    composable("word_of_day") {
                        WordOfTheDayScreen(
                            onNavigateBack = {
                                navController.navigate("dashboard")
                            }
                        )
                    }

                    // 3. Dashboard
                    composable("dashboard") {
                        DashboardScreen(
                            onNavigateToWordOfDay = { navController.navigate("word_of_day") },
                            onNavigateToCourseMap = { navController.navigate("course_map") },
                            onNavigateToPractice = { navController.navigate("practice") },
                            onNavigateToProgress = { navController.navigate("progress") },
                            onNavigateToDictionary = { navController.navigate("dictionary") },
                            onNavigateToCamera = { navController.navigate("camera") },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }

                    // 4. Mapa del curso
                    composable("course_map") {
                        CourseMapScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onLessonClick = { _ ->
                                navController.navigate("lessons")
                            }
                        )
                    }

                    // 5. Lecciones
                    composable("lessons") {
                        LessonsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onLessonClick = { _ ->
                                navController.navigate("practice")
                            }
                        )
                    }

                    // 6. Práctica
                    composable("practice") {
                        PracticeScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onCompleteExercise = {
                                navController.navigate("progress")
                            }
                        )
                    }

                    // 7. Progreso
                    composable("progress") {
                        ProgressScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // 8. Configuración (CORREGIDO)
                    composable("settings") {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    // 9. Diccionario
                    composable("dictionary") {
                        DictionaryScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onWordClick = { _ ->
                                // Detalle de palabra (próximamente)
                            }
                        )
                    }

                    // 10. Cámara / Traductor
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
