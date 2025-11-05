package com.signlearn.app.navigation

/**
 * Navigation - Rutas de navegación de SignLearn
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
}

fun String.toScreen(): Screen? = when (this) {
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
