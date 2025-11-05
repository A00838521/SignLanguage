package com.androidsignfigma.app.navigation

/**
 * Navigation - Sistema de navegación de SignLearn
 * Define todas las rutas y destinos de la aplicación
 */

/**
 * Sealed class que define todas las pantallas de la aplicación
 * Equivalente al tipo Screen de App.tsx
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object WordOfDay : Screen("word_of_day")
    object Dashboard : Screen("dashboard")
    object CourseMap : Screen("course_map")
    object Lessons : Screen("lessons")
    object Practice : Screen("practice")
    object Progress : Screen("progress")
    object Dictionary : Screen("dictionary")
    object Camera : Screen("camera")
    object Settings : Screen("settings")
    
    companion object {
        // Lista de todas las rutas para validación
        val allRoutes = listOf(
            Login.route,
            WordOfDay.route,
            Dashboard.route,
            CourseMap.route,
            Lessons.route,
            Practice.route,
            Progress.route,
            Dictionary.route,
            Camera.route,
            Settings.route
        )
    }
}

/**
 * Extensión para obtener el Screen desde un string de ruta
 */
fun String.toScreen(): Screen? {
    return when(this) {
        "login" -> Screen.Login
        "word_of_day", "word-of-day" -> Screen.WordOfDay
        "dashboard" -> Screen.Dashboard
        "course_map", "course-map" -> Screen.CourseMap
        "lessons" -> Screen.Lessons
        "practice" -> Screen.Practice
        "progress" -> Screen.Progress
        "dictionary" -> Screen.Dictionary
        "camera" -> Screen.Camera
        "settings" -> Screen.Settings
        else -> null
    }
}

/**
 * Extensión para navegar desde una pantalla string
 */
fun androidx.navigation.NavController.navigateToScreen(destination: String) {
    val screen = destination.toScreen()
    if (screen != null) {
        this.navigate(screen.route)
    }
}
