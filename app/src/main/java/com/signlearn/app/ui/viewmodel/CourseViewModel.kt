package com.signlearn.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.signlearn.app.data.firebase.LearningRepository
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.tasks.await
import com.signlearn.app.data.model.*
import com.signlearn.app.data.firebase.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CourseViewModel(private val repo: LearningRepository = LearningRepository()): ViewModel() {
    private val TAG = "CourseViewModel"
    private val _units = MutableStateFlow<List<LearningUnit>>(emptyList())
    val units: StateFlow<List<LearningUnit>> = _units

    private val _skills = MutableStateFlow<List<SkillCatalogEntry>>(emptyList())
    val skills: StateFlow<List<SkillCatalogEntry>> = _skills
    private val _unlockedSkills = MutableStateFlow<Set<String>>(emptySet())
    val unlockedSkills: StateFlow<Set<String>> = _unlockedSkills

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons
    private val _unlockedLessons = MutableStateFlow<Set<String>>(emptySet())
    val unlockedLessons: StateFlow<Set<String>> = _unlockedLessons

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    private val _categories = MutableStateFlow<List<com.signlearn.app.data.model.Category>>(emptyList())
    val categories: StateFlow<List<com.signlearn.app.data.model.Category>> = _categories
    private val _unlockedCategories = MutableStateFlow<Set<String>>(emptySet())
    val unlockedCategories: StateFlow<Set<String>> = _unlockedCategories

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        refreshUnits()
    }

    fun refreshCategories(uid: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val db = FirebaseFirestore.getInstance()
                val videosSnap = db.collection("videos").get().await()
                val videos = videosSnap.documents.mapNotNull { it.toObject(com.signlearn.app.data.model.SignVideo::class.java)?.copy(id = it.id) }
                val byCategory = videos.groupBy { it.category.ifBlank { "General" } }
                val list = byCategory.entries.map { (cat, listVideos) ->
                    val raw = cat
                    val title = sanitizeCategory(raw)
                    val slug = title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                    com.signlearn.app.data.model.Category(id = "cat_$slug", title = title, slug = slug, count = listVideos.size)
                }
                _categories.value = list
                Log.d(TAG, "refreshCategories: loaded ${list.size} categories")
                if (uid != null) {
                    val userRepo = UserRepository()
                    val completed = userRepo.getCompletedLessonsCount(uid)
                    val unlocked = mutableSetOf<String>()
                    list.sortedBy { it.title }.forEachIndexed { idx, cat ->
                        if (idx == 0) unlocked += cat.slug else if (completed >= idx) unlocked += cat.slug
                    }
                    _unlockedCategories.value = unlocked
                } else {
                    _unlockedCategories.value = list.map { it.slug }.toSet()
                }
            } catch (e: Exception) {
                Log.w(TAG, "refreshCategories: error loading categories", e)
                _categories.value = emptyList()
                _unlockedCategories.value = emptySet()
            }
            _loading.value = false
        }
    }

    // Sanitiza nombres de categoría para mostrar en UI y generar slugs predecibles
    private fun sanitizeCategory(raw: String): String {
        val base = raw.ifBlank { "General" }
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\bweb\\b", RegexOption.IGNORE_CASE), "")
            .trim()
        if (base.isEmpty()) return "General"
        return base.split(' ').joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    suspend fun ensureUserExercisesForCategory(uid: String, categorySlug: String): String? {
        // Intenta crear skill + ejercicios bajo users/{uid} pero captura errores de permisos para evitar crash.
        return try {
            val db = FirebaseFirestore.getInstance()
            Log.d(TAG, "ensureUserExercisesForCategory: start uid=$uid category=$categorySlug")
            val skillId = "user_skill_${uid}_$categorySlug"
            var title = categorySlug.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
            if (title.isNotEmpty()) title = title.substring(0,1).uppercase() + title.substring(1)
            val skillDoc = mapOf("title" to title, "categorySlug" to categorySlug, "createdAt" to System.currentTimeMillis())
            db.collection("users").document(uid).collection("skills").document(skillId).set(skillDoc).await()

            // Generar ejercicios simples (no reemplazamos el catálogo global)
            val allVideos = db.collection("videos").get().await().documents.mapNotNull { it.toObject(com.signlearn.app.data.model.SignVideo::class.java)?.copy(id = it.id) }
            val videos = allVideos.filter { it.category.ifBlank { "General" }.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_") == categorySlug }
            val mapped = videos.map { v -> v.title.replace(Regex("\\bWeb\\b", RegexOption.IGNORE_CASE), "").replace('_', ' ').replace("  +".toRegex(), " ").trim() }
            val titles = mapped.filter { it.isNotBlank() }.distinct()
            val chosen = if (titles.size >= 10) titles.shuffled().take(10) else {
                val res = mutableListOf<String>()
                if (titles.isEmpty()) res.add("-")
                while (res.size < 10) res.add(titles.randomOrNull() ?: "-")
                res
            }
            val LESSON_EXERCISES = 5
            val chunks = chosen.chunked(LESSON_EXERCISES)
            var lessonsCreated = 0
            var exercisesCreated = 0
            chunks.forEachIndexed { lessonIdx, chunk ->
                val lessonId = "user_${skillId}_lesson_${lessonIdx + 1}"
                val lessonDoc = mapOf("skillId" to skillId, "title" to "Lección ${lessonIdx + 1}: $title", "order" to (lessonIdx + 1))
                db.collection("users").document(uid).collection("skills").document(skillId).collection("lessons").document(lessonId).set(lessonDoc).await()
                lessonsCreated++
                chunk.forEachIndexed { exIdx, correctTitle ->
                    val correctVideo = videos.firstOrNull { it.title.replace(Regex("\\bWeb\\b", RegexOption.IGNORE_CASE), "").replace('_', ' ').trim() == correctTitle }
                    val distractPool = titles.filter { it != correctTitle }
                    val distractors = mutableListOf<String>()
                    val pool = distractPool.shuffled().toMutableList()
                    while (distractors.size < 3) {
                        if (pool.isEmpty()) distractors.add(titles.randomOrNull() ?: "-") else distractors.add(pool.removeAt(0))
                    }
                    val options = (distractors + correctTitle).shuffled()
                    val correctIndex = options.indexOf(correctTitle)
                    val exId = "user_${skillId}_ex_${lessonIdx + 1}_${exIdx + 1}"
                    val exDoc = mapOf(
                        "lessonId" to lessonId,
                        "prompt" to "Selecciona la palabra que corresponde a la seña:",
                        "options" to options,
                        "correctIndex" to correctIndex,
                        "xpReward" to 10,
                        "videoStoragePath" to (correctVideo?.storagePath ?: "")
                    )
                    db.collection("users").document(uid).collection("skills").document(skillId).collection("exercises").document(exId).set(exDoc).await()
                    exercisesCreated++
                }
            }
            Log.d(TAG, "ensureUserExercisesForCategory: created skill=$skillId lessons=$lessonsCreated exercises=$exercisesCreated")
            skillId
        } catch (e: Exception) {
            Log.w(TAG, "ensureUserExercisesForCategory: failed to create user exercises", e)
            null
        }
    }

    fun seedContent() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { repo.seedIfEmpty() }
            refreshUnits()
            _loading.value = false
        }
    }

    fun refreshUnits() {
        viewModelScope.launch {
            _loading.value = true
            val list = runCatching { repo.listUnits() }.getOrDefault(emptyList())
            _units.value = list
            _loading.value = false
        }
    }

    fun loadSkills(unitId: String, uid: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            val skillList = runCatching { repo.listSkillsForUnit(unitId) }.getOrDefault(emptyList())
            _skills.value = skillList
            // Gating: primer skill siempre desbloqueado; cada siguiente requiere todas sus lecciones completadas
            if (uid != null) {
                val userRepo = UserRepository()
                val completed = userRepo.getCompletedLessonIds(uid)
                val unlocked = mutableSetOf<String>()
                skillList.sortedBy { it.order }.forEachIndexed { index, skill ->
                    if (index == 0) {
                        unlocked += skill.id
                    } else {
                        // Obtener lecciones de skill anterior y verificar completadas
                        val prevSkill = skillList.sortedBy { it.order }[index - 1]
                        val prevLessons = runCatching { repo.listLessonsForSkill(prevSkill.id) }.getOrDefault(emptyList())
                        val allPrevCompleted = prevLessons.all { completed.contains(it.id) }
                        if (allPrevCompleted) unlocked += skill.id
                    }
                }
                _unlockedSkills.value = unlocked
            } else {
                _unlockedSkills.value = skillList.map { it.id }.toSet()
            }
            _lessons.value = emptyList()
            _loading.value = false
        }
    }

    fun loadLessons(skillId: String, uid: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            var lessonList: List<Lesson> = emptyList()
            try {
                Log.d(TAG, "loadLessons: skillId=$skillId uid=$uid")
                lessonList = repo.listLessonsForSkill(skillId)
                Log.d(TAG, "loadLessons: fetched ${'$'}{lessonList.size} lessons for skill=$skillId")
                _lessons.value = lessonList
            } catch (e: Exception) {
                Log.w(TAG, "loadLessons: error loading lessons for skill=$skillId", e)
                _lessons.value = emptyList()
                lessonList = emptyList()
            }
            if (uid != null) {
                val userRepo = UserRepository()
                val completed = userRepo.getCompletedLessonIds(uid)
                val unlocked = mutableSetOf<String>()
                lessonList.sortedBy { it.order }.forEachIndexed { index, lesson ->
                    if (index == 0) {
                        unlocked += lesson.id
                    } else {
                        val prevLesson = lessonList.sortedBy { it.order }[index - 1]
                        if (completed.contains(prevLesson.id)) unlocked += lesson.id
                    }
                }
                _unlockedLessons.value = unlocked
            } else {
                _unlockedLessons.value = lessonList.map { it.id }.toSet()
            }
            _exercises.value = emptyList()
            _loading.value = false
        }
    }

    fun loadExercises(lessonId: String) {
        viewModelScope.launch {
            _loading.value = true
            _exercises.value = runCatching { repo.listExercisesForLesson(lessonId) }.getOrDefault(emptyList())
            _loading.value = false
        }
    }
}
