package com.androidsignfigma.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.androidsignfigma.app.ui.screens.*
import com.androidsignfigma.app.ui.theme.SignLearnShapes

/**
 * SignLearnApp - Componente principal de la aplicación
 * 
 * Simula un dispositivo móvil (iPhone 14 Pro: 390x844px) con:
 * - Bordes redondeados
 * - Notch simulado
 * - Navegación entre pantallas
 * - Gestión de autenticación
 */
@Composable
fun SignLearnApp() {
    // Estado de navegación
    val navController = rememberNavController()
    
    // Estado de autenticación
    var isAuthenticated by remember { mutableStateOf(false) }
    
    // Fondo con gradiente sutil (simulando el contenedor gris del dispositivo)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0), // slate-200
                        Color(0xFFCBD5E1)  // slate-300
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
            
            // Contenido de la aplicación con scroll vertical
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Navegación principal
                NavHost(
                    navController = navController,
                    startDestination = if (isAuthenticated) "word_of_day" else "login"
                ) {
                    // Pantalla de Login
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
                    
                    // Pantalla Word of the Day
                    composable("word_of_day") {
                        WordOfTheDayScreen(
                            onContinue = {
                                navController.navigate("dashboard")
                            }
                        )
                    }
                    
                    // Dashboard principal
                    composable("dashboard") {
                        DashboardScreen(
                            onNavigate = { destination ->
                                navController.navigate(destination)
                            }
                        )
                    }
                    
                    // Mapa del curso
                    composable("course_map") {
                        CourseMapScreen(
                            onBack = { navController.popBackStack() },
                            onNavigate = { destination ->
                                navController.navigate(destination)
                            }
                        )
                    }
                    
                    // Lecciones
                    composable("lessons") {
                        LessonsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigate = { destination ->
                                navController.navigate(destination)
                            }
                        )
                    }
                    
                    // Práctica
                    composable("practice") {
                        PracticeScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    // Progreso y estadísticas
                    composable("progress") {
                        ProgressScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    // Diccionario
                    composable("dictionary") {
                        DictionaryScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    // Traductor con cámara
                    composable("camera") {
                        CameraTranslatorScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    
                    // Configuración
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onLogout = {
                                isAuthenticated = false
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Vista previa del diseño de la app
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp"
)
@Composable
fun SignLearnAppPreview() {
    SignLearnApp()
}
