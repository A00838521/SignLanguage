package com.signlearn.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.signlearn.app.data.model.*
import com.signlearn.app.ui.util.sanitizeTitle
import kotlinx.coroutines.tasks.await

class LearningRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun seedIfEmpty() {
        // Si no hay unidades, generar roadmap desde las categorías actuales en la colección `videos`.
        val unitsSnap = db.collection("units").limit(1).get().await()
        if (unitsSnap.isEmpty) {
            val videosSnap = db.collection("videos").get().await()
            val videos = videosSnap.documents.mapNotNull { doc ->
                doc.toObject(SignVideo::class.java)?.copy(id = doc.id)
            }
            val byCategory = videos.groupBy { it.category.ifBlank { "General" } }
            var unitOrder = 1
            byCategory.entries.forEach { (category, list) ->
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
                        val correctVideo = list.firstOrNull { sanitizeTitle(it.title) == correctTitle } ?: list.first()
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
                        val exercise = Exercise(id = exId, lessonId = lessonIdChunk, prompt = "Selecciona la palabra que corresponde a la seña:", options = options, correctIndex = correctIndex, xpReward = 10, videoStoragePath = correctVideo.storagePath)
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
}
