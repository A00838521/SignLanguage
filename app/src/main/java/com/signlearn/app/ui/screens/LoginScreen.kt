package com.signlearn.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import coil.compose.AsyncImage
import com.signlearn.app.ui.theme.*
import com.signlearn.app.R
import android.app.Activity
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginEmail: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onLoginGoogleIdToken: (String) -> Unit,
    onContinueAsGuest: () -> Unit,
    uiError: String? = null,
    uiLoading: Boolean = false
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                onLoginGoogleIdToken(idToken)
            } else {
                localError = "No se obtuvo ID token de Google"
            }
        } catch (e: Exception) {
            localError = e.message
        }
    }

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
        AsyncImage(
            model = "https://images.unsplash.com/photo-1739630405609-fd438c446f62",
            contentDescription = "Background",
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f)
        )

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

                Text(
                    text = "SignLearn",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Aprende Lengua de Señas Mexicana",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("tu@email.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = SignLearnShapes.InputField
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = SignLearnShapes.InputField
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) { TextButton(onClick = { }) { Text("¿Olvidaste tu contraseña?") } }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onLoginEmail(email.trim(), password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = SignLearnShapes.CategoryButton
                ) { Text("Iniciar sesión") }

                OutlinedButton(
                    onClick = { onRegister(email.trim(), password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = SignLearnShapes.CategoryButton
                ) { Text("Registrarse") }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (activity != null) {
                                scope.launch {
                                    isLoading = true
                                    localError = null
                                    try {
                                        val googleIdOption = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId(activity.getString(R.string.default_web_client_id))
                                            .build()
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleIdOption)
                                            .build()
                                        val credManager = CredentialManager.create(activity)
                                        val result = credManager.getCredential(activity, request)
                                        val credential = result.credential
                                        val googleTokenCred = GoogleIdTokenCredential.createFrom(credential.data)
                                        onLoginGoogleIdToken(googleTokenCred.idToken)
                                    } catch (e: GetCredentialCancellationException) {
                                        // usuario canceló
                                    } catch (e: GetCredentialException) {
                                        Log.e("LoginScreen", "CredMan error", e)
                                        // Fallback a GoogleSignInClient clásico
                                        try {
                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestIdToken(activity.getString(R.string.default_web_client_id))
                                                .requestEmail()
                                                .build()
                                            val client = GoogleSignIn.getClient(activity, gso)
                                            googleLauncher.launch(client.signInIntent)
                                        } catch (ex: Exception) {
                                            localError = ex.message
                                        }
                                    } catch (e: Exception) {
                                        localError = e.message
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = SignLearnShapes.CategoryButton
                    ) { Text("Google", style = MaterialTheme.typography.bodyMedium) }

                    OutlinedButton(
                        onClick = onContinueAsGuest,
                        modifier = Modifier.weight(1f),
                        shape = SignLearnShapes.CategoryButton
                    ) { Text("Invitado", style = MaterialTheme.typography.bodyMedium) }
                }

                val overallLoading = uiLoading || isLoading
                val mergedError = uiError ?: localError

                if (overallLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (mergedError != null) {
                    AssistChip(
                        onClick = { },
                        label = { Text(mergedError) },
                        leadingIcon = { Icon(Icons.Default.Error, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}
