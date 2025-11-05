package com.androidsignfigma.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.androidsignfigma.app.ui.theme.Background
import com.androidsignfigma.app.ui.theme.Border
import com.androidsignfigma.app.ui.theme.DarkBackground
import com.androidsignfigma.app.ui.theme.DarkBorder
import com.androidsignfigma.app.ui.theme.DarkMuted
import com.androidsignfigma.app.ui.theme.DarkMutedForeground
import com.androidsignfigma.app.ui.theme.DarkOnBackground
import com.androidsignfigma.app.ui.theme.DarkOnSurface
import com.androidsignfigma.app.ui.theme.DarkPrimary
import com.androidsignfigma.app.ui.theme.DarkSecondary
import com.androidsignfigma.app.ui.theme.DarkSurface
import com.androidsignfigma.app.ui.theme.DarkTertiary
import com.androidsignfigma.app.ui.theme.MutedForeground
import com.androidsignfigma.app.ui.theme.OnBackground
import com.androidsignfigma.app.ui.theme.OnError
import com.androidsignfigma.app.ui.theme.OnPrimary
import com.androidsignfigma.app.ui.theme.OnSecondary
import com.androidsignfigma.app.ui.theme.OnSurface
import com.androidsignfigma.app.ui.theme.OnTertiary
import com.androidsignfigma.app.ui.theme.Primary
import com.androidsignfigma.app.ui.theme.PrimaryLight
import com.androidsignfigma.app.ui.theme.PrimaryVariant
import com.androidsignfigma.app.ui.theme.Scrim
import com.androidsignfigma.app.ui.theme.Secondary
import com.androidsignfigma.app.ui.theme.SecondaryLight
import com.androidsignfigma.app.ui.theme.SecondaryVariant
import com.androidsignfigma.app.ui.theme.SurfaceVariant
import com.androidsignfigma.app.ui.theme.Tertiary
import com.androidsignfigma.app.ui.theme.TertiaryLight
import com.androidsignfigma.app.ui.theme.TertiaryVariant

// Esquema de colores claro (Light Theme)
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = OnPrimary,
    
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = OnSecondary,
    
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryLight,
    onTertiaryContainer = OnTertiary,
    
    background = Background,
    onBackground = OnBackground,
    
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = MutedForeground,
    
    error = Error,
    onError = OnError,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    outline = Border,
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Scrim,
    
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = PrimaryLight,
    
    surfaceTint = Primary,
)

// Esquema de colores oscuro (Dark Theme)
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = Color(0xFFFFFFFF),
    
    secondary = DarkSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = Color(0xFFFFFFFF),
    
    tertiary = DarkTertiary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = TertiaryVariant,
    onTertiaryContainer = Color(0xFFFFFFFF),
    
    background = DarkBackground,
    onBackground = DarkOnBackground,
    
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkMutedForeground,
    
    error = Error,
    onError = OnError,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    outline = DarkBorder,
    outlineVariant = Color(0xFF49454F),
    scrim = Scrim,
    
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Primary,
    
    surfaceTint = DarkPrimary,
)

/**
 * Tema principal de SignLearn
 * 
 * @param darkTheme Si debe usar el tema oscuro
 * @param dynamicColor Si debe usar colores dinámicos de Android 12+ (Material You)
 * @param content Contenido de la aplicación
 */
@Composable
fun SignLearnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Deshabilitado por defecto para usar paleta corporativa
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Colores dinámicos de Material You (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        // Tema oscuro personalizado
        darkTheme -> DarkColorScheme
        // Tema claro personalizado (por defecto)
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/**
 * Tema alternativo con colores dinámicos habilitados
 */
@Composable
fun SignLearnDynamicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    SignLearnTheme(
        darkTheme = darkTheme,
        dynamicColor = true,
        content = content
    )
}
