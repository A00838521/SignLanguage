// SettingsScreen.kt
// Pantalla de Configuración y Perfil - SOLO FRONTEND
// Sin conexión a base de datos, solo UI con datos mock

package com.signlearn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit = {}) {
    val scrollState = rememberScrollState()

    // Estados locales para los switches
    var dailyReminders by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var competitiveMode by remember { mutableStateOf(false) }

    // Estados para diálogos
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Datos mock del usuario
    val userName = "María González"
    val userEmail = "maria.gonzalez@email.com"
    val userBio = "Aprendiendo lengua de señas para comunicarme mejor con mi comunidad 🤟"
    val userLevel = 5
    val userStreak = 12
    val wordsLearned = 245
    val accuracy = 87
    val dailyGoal = 15

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configuración",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1e40af),
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(scrollState)
        ) {
            // Sección de Perfil
            ProfileSection(
                userName = userName,
                userEmail = userEmail,
                userBio = userBio,
                userLevel = userLevel,
                onEditClick = { showEditProfileDialog = true },
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Estadísticas Rápidas
            QuickStatsSection(
                streak = userStreak,
                wordsLearned = wordsLearned,
                accuracy = accuracy,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección de Aprendizaje
            SettingsSectionHeader("Aprendizaje")
            SettingsItem(
                icon = Icons.Outlined.Notifications,
                title = "Notificaciones",
                subtitle = "Recordatorios y alertas",
                onClick = { /* Navegar a notificaciones */ }
            )
            SettingsItemWithSwitch(
                icon = Icons.Outlined.NotificationsActive,
                title = "Recordatorios diarios",
                subtitle = "Practica todos los días a las 9:00 AM",
                checked = dailyReminders,
                onCheckedChange = { dailyReminders = it }
            )
            SettingsItemWithSwitch(
                icon = Icons.Outlined.VolumeUp,
                title = "Sonido",
                subtitle = "Efectos de sonido y audio",
                checked = soundEnabled,
                onCheckedChange = { soundEnabled = it }
            )
            SettingsItem(
                icon = Icons.Outlined.Language,
                title = "Idioma de interfaz",
                subtitle = "Español (México)",
                onClick = { /* Cambiar idioma */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección de Progreso
            SettingsSectionHeader("Progreso y Objetivos")
            SettingsItem(
                icon = Icons.Outlined.EmojiEvents,
                title = "Objetivos diarios",
                subtitle = "$dailyGoal minutos por día",
                onClick = { /* Editar objetivos */ }
            )
            SettingsItem(
                icon = Icons.Outlined.AutoGraph,
                title = "Estadísticas",
                subtitle = "Ver progreso detallado",
                onClick = { /* Navegar a estadísticas */ }
            )
            SettingsItemWithSwitch(
                icon = Icons.Outlined.Leaderboard,
                title = "Modo competitivo",
                subtitle = "Compite con otros usuarios",
                checked = competitiveMode,
                onCheckedChange = { competitiveMode = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección de Cuenta
            SettingsSectionHeader("Cuenta")
            SettingsItem(
                icon = Icons.Outlined.Security,
                title = "Privacidad y seguridad",
                subtitle = "Contraseña, datos personales",
                onClick = { /* Navegar a privacidad */ }
            )
            SettingsItem(
                icon = Icons.Outlined.CloudSync,
                title = "Sincronización",
                subtitle = "Respaldar progreso en la nube",
                onClick = { /* Configurar sincronización */ }
            )
            SettingsItem(
                icon = Icons.Outlined.DeleteOutline,
                title = "Eliminar cuenta",
                subtitle = "Borrar todos tus datos permanentemente",
                onClick = { /* Eliminar cuenta */ },
                textColor = Color(0xFFDC2626)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sección de Ayuda
            SettingsSectionHeader("Ayuda y Soporte")
            SettingsItem(
                icon = Icons.Outlined.Help,
                title = "Centro de ayuda",
                subtitle = "Preguntas frecuentes y tutoriales",
                onClick = { /* Navegar a ayuda */ }
            )
            SettingsItem(
                icon = Icons.Outlined.ContactSupport,
                title = "Contactar soporte",
                subtitle = "Enviar comentarios o reportar problemas",
                onClick = { /* Contactar soporte */ }
            )
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "Acerca de SignLearn",
                subtitle = "Versión 1.0.0",
                onClick = { /* Mostrar info de la app */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de Cerrar Sesión
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFDC2626)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFDC2626)
                )
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Cerrar sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Diálogo de Editar Perfil
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentBio = userBio,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, bio ->
                // Aquí guardarías los datos
                showEditProfileDialog = false
            }
        )
    }

    // Diálogo de Confirmar Cierre de Sesión
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    tint = Color(0xFFDC2626)
                )
            },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        // Aquí harías logout
                    }
                ) {
                    Text("Cerrar sesión", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ProfileSection(
    userName: String,
    userEmail: String,
    userBio: String,
    userLevel: Int,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Foto de perfil (inicial del nombre)
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF1e40af))
                        .border(3.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Botón de editar
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onEditClick() },
                    shape = CircleShape,
                    color = Color(0xFF1e40af),
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar perfil",
                        modifier = Modifier.padding(6.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre
            Text(
                text = userName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Email
            Text(
                text = userEmail,
                fontSize = 14.sp,
                color = Color(0xFF64748b)
            )

            if (userBio.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = userBio,
                    fontSize = 14.sp,
                    color = Color(0xFF475569),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badge de nivel
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF065f46).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF065f46),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Nivel $userLevel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF065f46)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickStatsSection(
    streak: Int,
    wordsLearned: Int,
    accuracy: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            title = "Racha",
            value = "$streak",
            subtitle = "días",
            icon = Icons.Default.LocalFireDepartment,
            color = Color(0xFFEA580C),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Palabras",
            value = "$wordsLearned",
            subtitle = "aprendidas",
            icon = Icons.Default.MenuBook,
            color = Color(0xFF1e40af),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Precisión",
            value = "$accuracy%",
            subtitle = "global",
            icon = Icons.Default.TrendingUp,
            color = Color(0xFF065f46),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748b)
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF64748b),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    textColor: Color = Color(0xFF0F172A)
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF64748b),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF64748b)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsItemWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF64748b),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF64748b)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF1e40af),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    currentName: String,
    currentBio: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar perfil") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biografía") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, bio) },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
