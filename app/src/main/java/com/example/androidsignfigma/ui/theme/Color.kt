package com.androidsignfigma.app.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de colores profesional de SignLearn
// Basada en Material Design con azul corporativo, gris slate y verde oscuro

// Colores principales
val Primary = Color(0xFF1E40AF)        // Azul corporativo #1e40af
val PrimaryVariant = Color(0xFF1E3A8A) // Azul más oscuro
val PrimaryLight = Color(0xFF3B82F6)   // Azul más claro

val Secondary = Color(0xFF64748B)      // Gris slate #64748b
val SecondaryVariant = Color(0xFF475569)
val SecondaryLight = Color(0xFF94A3B8)

val Tertiary = Color(0xFF065F46)       // Verde oscuro #065f46
val TertiaryVariant = Color(0xFF047857)
val TertiaryLight = Color(0xFF10B981)

// Colores de fondo y superficie
val Background = Color(0xFFFFFFFF)     // Blanco
val BackgroundGradientStart = Color(0xFFF8FAFC)
val BackgroundGradientEnd = Color(0xFFF1F5F9)

val Surface = Color(0xFFFFFFFF)        // Blanco
val SurfaceVariant = Color(0xFFF8FAFC)

// Colores de texto
val OnPrimary = Color(0xFFFFFFFF)      // Blanco sobre primario
val OnSecondary = Color(0xFFFFFFFF)    // Blanco sobre secundario
val OnTertiary = Color(0xFFFFFFFF)     // Blanco sobre terciario
val OnBackground = Color(0xFF1A1A1A)   // Negro sobre fondo
val OnSurface = Color(0xFF1A1A1A)      // Negro sobre superficie

// Colores funcionales
val Error = Color(0xFFDC2626)          // Rojo para errores
val OnError = Color(0xFFFFFFFF)
val Success = Color(0xFF059669)        // Verde para éxito
val Warning = Color(0xFFF59E0B)        // Amarillo para advertencias
val Info = Color(0xFF0EA5E9)           // Azul para información

// Colores adicionales para la UI
val Muted = Color(0xFFF1F5F9)
val MutedForeground = Color(0xFF64748B)
val Border = Color(0xFFE2E8F0)
val InputBackground = Color(0xFFF8FAFC)
val SwitchBackground = Color(0xFFCBD5E1)

// Colores para gráficos (basados en la paleta)
val Chart1 = Color(0xFF1E40AF)         // Azul corporativo
val Chart2 = Color(0xFF0891B2)         // Cyan
val Chart3 = Color(0xFF059669)         // Verde
val Chart4 = Color(0xFF475569)         // Gris oscuro
val Chart5 = Color(0xFF0F766E)         // Teal

// Colores para estados de lecciones
val LessonCompleted = Color(0xFF059669)
val LessonInProgress = Color(0xFF1E40AF)
val LessonLocked = Color(0xFF9CA3AF)

// Colores para badges y categorías
val CategoryBasic = Color(0xFF3B82F6)
val CategoryIntermediate = Color(0xFF8B5CF6)
val CategoryAdvanced = Color(0xFFF97316)

// Colores de overlay y sombreado
val Scrim = Color(0x80000000)          // Negro con 50% opacidad
val Overlay = Color(0x1A1E40AF)        // Primario con 10% opacidad

// Gradientes (se usarán con Brush)
// Ejemplo: Brush.linearGradient(listOf(GradientPrimaryStart, GradientPrimaryEnd))
val GradientPrimaryStart = Color(0xFF1E40AF)
val GradientPrimaryEnd = Color(0xFF64748B)

val GradientAccentStart = Color(0xFF065F46)
val GradientAccentEnd = Color(0xFF10B981)

val GradientBackgroundStart = Color(0xFFF8FAFC)
val GradientBackgroundEnd = Color(0xFFE2E8F0)

// Colores para modo oscuro (Dark theme)
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkPrimary = Color(0xFF3B82F6)
val DarkSecondary = Color(0xFF64748B)
val DarkTertiary = Color(0xFF10B981)
val DarkOnBackground = Color(0xFFF8FAFC)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkMuted = Color(0xFF334155)
val DarkMutedForeground = Color(0xFF94A3B8)
val DarkBorder = Color(0xFF334155)
