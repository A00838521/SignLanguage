package com.androidsignfigma.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.androidsignfigma.app.ui.theme.*

/**
 * LoginScreen - Pantalla de inicio de sesión de SignLearn
 * 
 * Características:
 * - Fondo con gradiente sutil
 * - Animaciones de manos flotantes
 * - Formulario de login/registro
 * - Integración con redes sociales
 * 
 * Traducción de: LoginScreen.tsx
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: () -> Unit
) {
    // Estado para los campos de texto
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Animaciones infinitas para las manos flotantes
    val infiniteTransition = rememberInfiniteTransition(label = "handAnimation")
    
    val hand1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hand1Y"
    )
    
    val hand1Rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hand1Rotation"
    )
    
    val hand2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hand2Y"
    )
    
    val hand2Rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hand2Rotation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background,
                        Tertiary.copy(alpha = 0.05f)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Imagen de fondo con opacidad
        AsyncImage(
            model = "https://images.unsplash.com/photo-1739630405609-fd438c446f62",
            contentDescription = "Background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f)
        )
        
        // Mano flotante 1 (superior izquierda)
        Icon(
            imageVector = Icons.Default.PanTool,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.TopStart)
                .offset(x = 24.dp, y = (32 + hand1Offset).dp)
                .rotate(hand1Rotation)
                .alpha(0.2f),
            tint = Primary
        )
        
        // Mano flotante 2 (inferior derecha)
        Icon(
            imageVector = Icons.Default.PanTool,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-24).dp, y = (-128 + hand2Offset).dp)
                .rotate(hand2Rotation)
                .alpha(0.2f),
            tint = Tertiary
        )
        
        // Card de login
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = SignLearnShapes.CardElevated
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo e icono
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = SignLearnShapes.CardElevated,
                    color = Primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PanTool,
                            contentDescription = "SignLearn Logo",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                }
                
                // Título
                Text(
                    text = "SignLearn",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Subtítulo
                Text(
                    text = "Aprende Lengua de Señas Mexicana",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Campo de email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("tu@email.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = SignLearnShapes.InputField
                )
                
                // Campo de contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = SignLearnShapes.InputField
                )
                
                // Olvidaste contraseña
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { /* TODO */ }) {
                        Text("¿Olvidaste tu contraseña?")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Botón de inicio de sesión
                Button(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    ),
                    shape = SignLearnShapes.CategoryButton
                ) {
                    Text("Iniciar sesión")
                }
                
                // Botón de registro
                OutlinedButton(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = SignLearnShapes.CategoryButton
                ) {
                    Text("Registrarse")
                }
                
                // Divider con texto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "O continúa con",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
                
                // Botones de redes sociales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLogin,
                        modifier = Modifier.weight(1f),
                        shape = SignLearnShapes.CategoryButton
                    ) {
                        // Ícono de Google (simplificado)
                        Text("Google", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    OutlinedButton(
                        onClick = onLogin,
                        modifier = Modifier.weight(1f),
                        shape = SignLearnShapes.CategoryButton
                    ) {
                        // Ícono de Facebook (simplificado)
                        Text("Facebook", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/**
 * Vista previa de la pantalla de login
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    device = "spec:width=390dp,height=844dp"
)
@Composable
fun LoginScreenPreview() {
    SignLearnTheme {
        LoginScreen(onLogin = {})
    }
}
