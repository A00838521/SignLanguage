package com.signlearn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.signlearn.app.ui.theme.SignLearnTheme
import com.google.firebase.FirebaseApp

/**
 * MainActivity - Punto de entrada de la aplicación SignLearn
 *
 * Configura:
 * - Edge-to-edge display (pantalla completa)
 * - Splash screen
 * - Tema de la aplicación
 * - Navegación principal
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar splash screen antes de super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Habilitar modo edge-to-edge (usar toda la pantalla)
        enableEdgeToEdge()

        // Inicializar Firebase (utiliza google-services.json)
        FirebaseApp.initializeApp(this)

        setContent {
            // Aplicar el tema SignLearn
            SignLearnTheme {
                // Surface principal con el color de fondo del tema
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Iniciar la aplicación principal
                    SignLearnApp()
                }
            }
        }
    }
}
