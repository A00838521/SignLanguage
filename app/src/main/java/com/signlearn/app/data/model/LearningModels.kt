package com.signlearn.app.data.model

import com.google.firebase.firestore.DocumentId

// Unidad de aprendizaje (agrupa skills)
data class LearningUnit(
    @DocumentId val id: String = "",
    val title: String = "",
    val order: Int = 0,
    val description: String = ""
)

// Skill (habilidad temática dentro de una unidad)
data class SkillCatalogEntry(
    @DocumentId val id: String = "",
    val unitId: String = "",
    val title: String = "",
    val order: Int = 0,
    val icon: String = ""
)

// Lección dentro de una skill
data class Lesson(
    @DocumentId val id: String = "",
    val skillId: String = "",
    val title: String = "",
    val order: Int = 0,
    val estimatedMinutes: Int = 5
)

// Ejercicio de una lección (tipo simplificado)
data class Exercise(
    @DocumentId val id: String = "",
    val lessonId: String = "",
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val xpReward: Int = 10,
    // Campo legado para ejercicios existentes con solo video
    val videoStoragePath: String? = null,
    // Tipo de medio principal: "video" | "image" (default video para compatibilidad)
    val mediaType: String = "video",
    // Ruta en Storage del medio (video o imagen)
    val mediaStoragePath: String? = null
)

// Progreso de skill para usuario
data class UserSkillProgress(
    @DocumentId val id: String = "",
    val skillId: String = "",
    val mastery: Int = 0 // 0-100
)

// Categoría derivada de los videos (para el mapa del curso dinámico)
data class Category(
    val id: String = "",
    val title: String = "",
    val slug: String = "",
    val count: Int = 0
)
