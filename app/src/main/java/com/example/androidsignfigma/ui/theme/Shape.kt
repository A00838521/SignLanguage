package com.androidsignfigma.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas (Shapes) de SignLearn basadas en Material Design 3
 * Define los bordes redondeados para diferentes componentes
 * 
 * Basado en --radius: 0.5rem (8dp) de globals.css
 */
val Shapes = Shapes(
    // Extra Small - Para elementos muy pequeños (chips, badges)
    extraSmall = RoundedCornerShape(4.dp), // --radius-sm: calc(var(--radius) - 4px)
    
    // Small - Para botones pequeños, inputs
    small = RoundedCornerShape(6.dp), // --radius-md: calc(var(--radius) - 2px)
    
    // Medium - Para la mayoría de componentes (cards, buttons)
    medium = RoundedCornerShape(8.dp), // --radius-lg: var(--radius) = 0.5rem = 8dp
    
    // Large - Para cards grandes, dialogs
    large = RoundedCornerShape(12.dp), // --radius-xl: calc(var(--radius) + 4px)
    
    // Extra Large - Para modales, bottom sheets
    extraLarge = RoundedCornerShape(16.dp),
)

// Shapes personalizadas adicionales para SignLearn
object SignLearnShapes {
    // Para el contenedor de la app móvil (simula iPhone)
    val PhoneContainer = RoundedCornerShape(40.dp)
    
    // Para notch/dynamic island del teléfono
    val PhoneNotch = RoundedCornerShape(
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )
    
    // Para cards con sombra elevada
    val CardElevated = RoundedCornerShape(12.dp)
    
    // Para elementos completamente circulares (avatares, FABs)
    val Circle = RoundedCornerShape(50)
    
    // Para pills/badges
    val Pill = RoundedCornerShape(100.dp)
    
    // Para bottom navigation bar
    val BottomBar = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp
    )
    
    // Para top app bar con esquinas inferiores redondeadas
    val TopBar = RoundedCornerShape(
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )
    
    // Para overlay de cámara (área de detección)
    val CameraOverlay = RoundedCornerShape(8.dp)
    
    // Para progress cards con gradiente
    val ProgressCard = RoundedCornerShape(16.dp)
    
    // Para achievement badges
    val AchievementBadge = RoundedCornerShape(12.dp)
    
    // Para lesson cards en el mapa del curso
    val LessonCard = RoundedCornerShape(12.dp)
    
    // Para dictionary word cards
    val DictionaryCard = RoundedCornerShape(10.dp)
    
    // Para botones de categoría
    val CategoryButton = RoundedCornerShape(8.dp)
    
    // Para video/animation containers
    val VideoContainer = RoundedCornerShape(12.dp)
    
    // Para estadísticas y gráficos
    val ChartCard = RoundedCornerShape(16.dp)
    
    // Para inputs con fondo
    val InputField = RoundedCornerShape(8.dp)
    
    // Para dialogs y bottom sheets
    val Dialog = RoundedCornerShape(24.dp)
    
    // Para floating action buttons extendidos
    val ExtendedFab = RoundedCornerShape(16.dp)
    
    // Sin redondeo (para algunos casos específicos)
    val None = RoundedCornerShape(0.dp)
    
    // Redondeado solo arriba (para modales que salen desde abajo)
    val BottomSheet = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Redondeado solo abajo (para headers sticky)
    val StickyHeader = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )
}

// Utilidades para bordes personalizados
object BorderWidths {
    val Thin = 1.dp
    val Medium = 2.dp
    val Thick = 4.dp
    val ExtraThick = 8.dp
}

// Elevaciones (sombras) para diferentes componentes
object Elevations {
    val None = 0.dp
    val ExtraSmall = 1.dp
    val Small = 2.dp
    val Medium = 4.dp
    val Large = 8.dp
    val ExtraLarge = 16.dp
}
