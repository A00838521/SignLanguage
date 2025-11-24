package com.signlearn.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.signlearn.app.data.model.*
import com.signlearn.app.ui.util.sanitizeTitle
import kotlinx.coroutines.tasks.await

class LearningRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    companion object {
        // Orden pedagógico sugerido
        val LEARNING_CATEGORY_ORDER = listOf(
            "abecedario",
            "saludoscortesias",
            "numero",
            "colores",
            "pronombres",
            "personas",
            "preguntas",
            "diasdelasemana",
            "mesesdelanio",
            "tiempo",
            "animales",
            "comida",
            "frutas",
            "verduras",
            "cuerpo",
            "hogar",
            "ropa",
            "transporte",
            "lugares",
            "puestosprofesionesoficios",
            "verboscomunes",
            "verbosnarrativos",
            "preposicionesadjetivossustantivosadverbios"
        )
    }
    suspend fun seedIfEmpty() {
        // Si no hay unidades, generar roadmap desde las categorías actuales en la colección `videos`.
        val unitsSnap = db.collection("units").limit(1).get().await()
        if (unitsSnap.isEmpty) {
            val videosSnap = db.collection("videos").get().await()
            val imagesSnap = runCatching { db.collection("images").get().await() }.getOrNull()
            val videos = videosSnap.documents.mapNotNull { doc ->
                doc.toObject(SignVideo::class.java)?.copy(id = doc.id)
            }
            val images = imagesSnap?.documents?.mapNotNull { doc ->
                doc.toObject(SignImage::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            // Unificar por categoría (videos + imágenes) para ejercicios mixtos
            data class Media(val title: String, val storagePath: String, val category: String, val type: String)
            val mediaAll = videos.map { Media(it.title, it.storagePath, it.category.ifBlank { "General" }, "video") } +
                images.map { Media(it.title, it.storagePath, it.category.ifBlank { "General" }, "image") }
            val byCategory = mediaAll.groupBy { it.category }
            val orderedExisting = LEARNING_CATEGORY_ORDER.filter { byCategory.containsKey(it) }
            val remaining = byCategory.keys.filterNot { orderedExisting.contains(it) }.sorted()
            val finalOrder = orderedExisting + remaining
            var unitOrder = 1
            finalOrder.forEach { category ->
                val list = byCategory[category] ?: return@forEach
                val slug = category.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_")
                val unitId = "unit_$slug"
                val unitTitle = category.replace('_', ' ').replace(Regex("\\s+"), " ").trim().replaceFirstChar { it.uppercaseChar().toString() }
                val unit = LearningUnit(id = unitId, title = unitTitle, order = unitOrder++, description = "Lecciones de $category")
                db.collection("units").document(unitId).set(unit).await()

                val skillId = "skill_$slug"
                val skill = SkillCatalogEntry(id = skillId, unitId = unitId, title = unit.title, order = 1, icon = "category")
                db.collection("skillsCatalog").document(skillId).set(skill).await()

                // Generar ejercicios: tomar hasta 10 palabras por categoría y dividir en lecciones (bloques)
                val titles = list.map { sanitizeTitle(it.title) }.filter { it.isNotBlank() }.distinct()
                val chosen = if (titles.size >= 10) titles.shuffled().take(10) else {
                    val res = mutableListOf<String>()
                    if (titles.isEmpty()) res.add("-")
                    while (res.size < 10) {
                        res.add(titles.randomOrNull() ?: "-")
                    }
                    res
                }

                val LESSON_EXERCISES = 5
                val chunks = chosen.chunked(LESSON_EXERCISES)
                chunks.forEachIndexed { lessonIdx, chunk ->
                    val lessonIdChunk = "lesson_${slug}_${lessonIdx + 1}"
                    val lesson = Lesson(id = lessonIdChunk, skillId = skillId, title = "Lección ${lessonIdx + 1}: ${unit.title}", order = lessonIdx + 1, estimatedMinutes = (chunk.size * 1))
                    db.collection("lessons").document(lessonIdChunk).set(lesson).await()
                    chunk.forEachIndexed { exIdx, correctTitle ->
                        val correctMedia = list.firstOrNull { sanitizeTitle(it.title) == correctTitle } ?: list.first()
                        val distractPool = titles.filter { it != correctTitle }
                        val distractors = mutableListOf<String>()
                        val needed = 3
                        val pool = distractPool.shuffled().toMutableList()
                        while (distractors.size < needed) {
                            if (pool.isEmpty()) {
                                distractors.add(titles.randomOrNull() ?: "-")
                            } else {
                                distractors.add(pool.removeAt(0))
                            }
                        }
                        val options = (distractors + correctTitle).shuffled()
                        val correctIndex = options.indexOf(correctTitle)
                        val exId = "ex_${slug}_${lessonIdx + 1}_${exIdx + 1}"
                        val exercise = Exercise(
                            id = exId,
                            lessonId = lessonIdChunk,
                            prompt = "Selecciona la palabra que corresponde a la seña:",
                            options = options,
                            correctIndex = correctIndex,
                            xpReward = 10,
                            videoStoragePath = if (correctMedia.type == "video") correctMedia.storagePath else null,
                            mediaType = correctMedia.type,
                            mediaStoragePath = correctMedia.storagePath
                        )
                        db.collection("exercises").document(exId).set(exercise).await()
                    }
                }
            }
        }
    }

    suspend fun listUnits(): List<LearningUnit> = db.collection("units").get().await().toObjects(LearningUnit::class.java).sortedBy { it.order }
    suspend fun listSkillsForUnit(unitId: String): List<SkillCatalogEntry> = db.collection("skillsCatalog").whereEqualTo("unitId", unitId).get().await().toObjects(SkillCatalogEntry::class.java).sortedBy { it.order }
    suspend fun listLessonsForSkill(skillId: String): List<Lesson> {
        return if (skillId.startsWith("user_skill_")) {
            // skillId format: user_skill_{uid}_{categorySlug...}
            val tail = skillId.removePrefix("user_skill_")
            val idx = tail.indexOf('_')
            val uid = if (idx >= 0) tail.substring(0, idx) else tail
            db.collection("users").document(uid).collection("skills").document(skillId).collection("lessons").get().await().toObjects(Lesson::class.java).sortedBy { it.order }
        } else {
            db.collection("lessons").whereEqualTo("skillId", skillId).get().await().toObjects(Lesson::class.java).sortedBy { it.order }
        }
    }

    suspend fun listExercisesForLesson(lessonId: String): List<Exercise> {
        return if (lessonId.startsWith("user_")) {
            // lessonId format: user_{skillId}_lesson_{N}
            val after = lessonId.removePrefix("user_")
            val skillId = after.substringBefore("_lesson_")
            if (skillId.startsWith("user_skill_")) {
                val tail = skillId.removePrefix("user_skill_")
                val idx = tail.indexOf('_')
                val uid = if (idx >= 0) tail.substring(0, idx) else tail
                db.collection("users").document(uid).collection("skills").document(skillId).collection("exercises").whereEqualTo("lessonId", lessonId).get().await().toObjects(Exercise::class.java)
            } else {
                // fallback: try global collection
                db.collection("exercises").whereEqualTo("lessonId", lessonId).get().await().toObjects(Exercise::class.java)
            }
        } else {
            db.collection("exercises").whereEqualTo("lessonId", lessonId).get().await().toObjects(Exercise::class.java)
        }
    }

    /**
     * Genera contenido de skill (lecciones + ejercicios) específico para el usuario si no existe.
     * Combina videos e imágenes para la categoría dada.
     */
    suspend fun ensureUserSkillContent(uid: String, categorySlug: String) {
        val skillId = "user_skill_${uid}_${categorySlug}"
        // Verificar si ya tiene lecciones
        val lessonsSnap = db.collection("users").document(uid).collection("skills").document(skillId).collection("lessons").limit(1).get().await()
        if (!lessonsSnap.isEmpty) return

        // Reunir media pública de esa categoría (videos + imágenes)
        val videosSnap = db.collection("videos").whereEqualTo("category", categorySlug).get().await()
        val imagesSnap = runCatching { db.collection("images").whereEqualTo("category", categorySlug).get().await() }.getOrNull()
        val videos = videosSnap.documents.mapNotNull { it.toObject(SignVideo::class.java)?.copy(id = it.id) }
        val images = imagesSnap?.documents?.mapNotNull { it.toObject(SignImage::class.java)?.copy(id = it.id) } ?: emptyList()
        if (videos.isEmpty() && images.isEmpty()) return
        data class Media(val title: String, val storagePath: String, val type: String)
        val mediaAll = videos.map { Media(it.title, it.storagePath, "video") } + images.map { Media(it.title, it.storagePath, "image") }

        // Crear documento de skill (solo metadatos mínimos)
        val skillDoc = mapOf(
            "id" to skillId,
            "unitId" to "user_unit_${uid}", // unidad ficticia local
            "title" to categorySlug.replace('_', ' ').replaceFirstChar { it.uppercaseChar().toString() },
            "order" to 1,
            "icon" to "category"
        )
        db.collection("users").document(uid).collection("skills").document(skillId).set(skillDoc).await()

        // Generar lecciones y ejercicios (5 ejercicios por lección, máximo 10 ítems base)
        val titles = mediaAll.map { sanitizeTitle(it.title) }.filter { it.isNotBlank() }.distinct()
        val basePool = if (titles.size >= 10) titles.shuffled().take(10) else {
            val res = mutableListOf<String>()
            if (titles.isEmpty()) res.add("-")
            while (res.size < 10) {
                res.add(titles.randomOrNull() ?: "-")
            }
            res
        }
        val LESSON_EXERCISES = 5
        val chunks = basePool.chunked(LESSON_EXERCISES)
        chunks.forEachIndexed { lessonIdx, chunk ->
            val lessonId = "user_${skillId}_lesson_${lessonIdx + 1}"
            val lessonDoc = mapOf(
                "id" to lessonId,
                "skillId" to skillId,
                "title" to "Lección ${lessonIdx + 1}: ${skillDoc["title"]}",
                "order" to (lessonIdx + 1),
                "estimatedMinutes" to chunk.size
            )
            db.collection("users").document(uid).collection("skills").document(skillId).collection("lessons").document(lessonId).set(lessonDoc).await()
            chunk.forEachIndexed { exIdx, correctTitle ->
                val correctMedia = mediaAll.firstOrNull { sanitizeTitle(it.title) == correctTitle } ?: mediaAll.first()
                val distractPool = titles.filter { it != correctTitle }
                val distractors = mutableListOf<String>()
                val needed = 3
                val pool = distractPool.shuffled().toMutableList()
                while (distractors.size < needed) {
                    if (pool.isEmpty()) distractors.add(titles.randomOrNull() ?: "-") else distractors.add(pool.removeAt(0))
                }
                val options = (distractors + correctTitle).shuffled()
                val correctIndex = options.indexOf(correctTitle)
                val exerciseId = "user_ex_${skillId}_${lessonIdx + 1}_${exIdx + 1}"
                val exercise = Exercise(
                    id = exerciseId,
                    lessonId = lessonId,
                    prompt = "Selecciona la palabra que corresponde a la seña:",
                    options = options,
                    correctIndex = correctIndex,
                    xpReward = 10,
                    videoStoragePath = if (correctMedia.type == "video") correctMedia.storagePath else null,
                    mediaType = correctMedia.type,
                    mediaStoragePath = correctMedia.storagePath
                )
                db.collection("users").document(uid).collection("skills").document(skillId).collection("exercises").document(exerciseId).set(exercise).await()
            }
        }
    }

    /** Devuelve la primera categoría disponible siguiendo el orden pedagógico. */
    suspend fun pickFirstOrderedCategory(): String? {
        val videosSnap = db.collection("videos").get().await()
        val imagesSnap = runCatching { db.collection("images").get().await() }.getOrNull()
        val videoCats = videosSnap.documents.mapNotNull { it.getString("category") }.filter { it.isNotBlank() }.toSet()
        val imageCats = imagesSnap?.documents?.mapNotNull { it.getString("category") }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        val all = videoCats + imageCats
        return LEARNING_CATEGORY_ORDER.firstOrNull { all.contains(it) } ?: all.firstOrNull()
    }
}
