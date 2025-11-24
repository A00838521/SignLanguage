package com.signlearn.app.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.signlearn.app.data.model.UserProgress
import com.signlearn.app.data.model.UserProfile
import com.signlearn.app.data.model.WeeklyStats
import com.signlearn.app.data.model.UserPracticeState
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun addXP(uid: String, amount: Int) {
        val doc = db.collection("users").document(uid)
        val snap = runCatching { doc.get().await() }.getOrNull()
        val now = System.currentTimeMillis()
        if (snap == null || !snap.exists()) {
            val profile = UserProfile(uid = uid, totalPoints = amount, streak = 1, dailyGoal = 50, lastActive = now, xpHistory = listOf(amount))
            doc.set(profile).await()
            return
        }
        val lastActive = snap.getLong("lastActive") ?: now
        val historyAny = snap.get("xpHistory") as? List<*> ?: emptyList<Int>()
        val history = historyAny.mapNotNull { (it as? Number)?.toInt() }
        val dayDiff = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now - lastActive)
        val updatedHistory = if (dayDiff == 0L) {
            if (history.isEmpty()) listOf(amount) else history.dropLast(1) + (history.last() + amount)
        } else {
            (history + amount).takeLast(14)
        }
        val updateMap = mutableMapOf<String, Any>(
            "totalPoints" to FieldValue.increment(amount.toLong()),
            "xpHistory" to updatedHistory
        )
        if (dayDiff != 0L) updateMap["lastActive"] = now
        doc.set(updateMap, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun markLessonCompleted(uid: String, lessonId: String, score: Int) {
        val progress = UserProgress(lessonId = lessonId, score = score, completedAt = System.currentTimeMillis())
        db.collection("users").document(uid)
            .collection("progress").document(lessonId)
            .set(progress).await()
    }

    suspend fun ensureUserProfile(uid: String, displayName: String?, email: String?) {
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        if (!snap.exists()) {
            val now = System.currentTimeMillis()
            val profile = UserProfile(
                uid = uid,
                displayName = displayName,
                email = email,
                totalPoints = 0,
                streak = 1,
                dailyGoal = 50,
                lastActive = now,
                xpHistory = emptyList()
            )
            userRef.set(profile).await()
        } else {
            // Solo actualiza datos básicos si han cambiado, no recalcula streak aquí
            val update = mutableMapOf<String, Any>()
            if (displayName != null) update["displayName"] = displayName
            if (email != null) update["email"] = email
            if (update.isNotEmpty()) {
                userRef.set(update, SetOptions.merge()).await()
            }
        }
    }

    suspend fun updateStreak(uid: String) {
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        if (!snap.exists()) return
        val now = System.currentTimeMillis()
        val lastActive = snap.getLong("lastActive") ?: now
        val currentStreak = snap.getLong("streak")?.toInt() ?: 0
        val daysDiff = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now - lastActive)
        val newStreak = when (daysDiff) {
            0L -> currentStreak.coerceAtLeast(1) // ya activo hoy
            1L -> if (currentStreak <= 0) 1 else currentStreak + 1 // continuidad
            else -> 1 // reinicio
        }
        userRef.set(
            mapOf(
                "streak" to newStreak,
                "lastActive" to now
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val snap = db.collection("users").document(uid).get().await()
        return snap.toObject(UserProfile::class.java)
    }

    suspend fun getCompletedLessonsCount(uid: String): Int {
        return try {
            val snap = db.collection("users").document(uid).collection("progress").get().await()
            snap.size()
        } catch (e: Exception) {
            // Silenciar PERMISSION_DENIED y retornar 0 para evitar spam de log
            0
        }
    }

    suspend fun getCompletedLessonIds(uid: String): Set<String> {
        return try {
            db.collection("users").document(uid).collection("progress").get().await().documents.mapNotNull { it.id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun getWeeklyStats(uid: String): WeeklyStats {
        return try {
            val now = System.currentTimeMillis()
            val weekAgo = now - 7L * 24L * 60L * 60L * 1000L
            val progressSnap = db.collection("users").document(uid).collection("progress").get().await()
            val progress = progressSnap.documents.mapNotNull { it.toObject(UserProgress::class.java) }
                .filter { it.completedAt >= weekAgo }
            val lessonsCompleted = progress.size
            val scores = progress.map { it.score }.filter { it > 0 }
            val avgScore = if (scores.isNotEmpty()) scores.average().toInt().coerceIn(0, 100) else 0
            val profile = getUserProfile(uid)
            val xpHistory = profile?.xpHistory ?: emptyList()
            val xpWeek = xpHistory.takeLast(7).sum()
            // Estimación: 10 XP ≈ 1 ejercicio, 1 ejercicio ≈ 30s => 2 ejercicios/min
            val practiceMinutes = if (xpWeek > 0) ((xpWeek / 10.0) / 2.0).toInt().coerceAtLeast(1) else 0
            WeeklyStats(
                lessonsCompleted = lessonsCompleted,
                totalXp = xpWeek,
                averageScore = avgScore,
                practiceMinutes = practiceMinutes
            )
        } catch (e: Exception) {
            WeeklyStats()
        }
    }

    suspend fun getPracticeState(uid: String): UserPracticeState? {
        return try {
            db.collection("users").document(uid)
                .collection("state").document("practice")
                .get().await().toObject(UserPracticeState::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun setPracticeState(uid: String, lessonId: String, exerciseIndex: Int) {
        val state = UserPracticeState(lessonId = lessonId, exerciseIndex = exerciseIndex, updatedAt = System.currentTimeMillis())
        runCatching {
            db.collection("users").document(uid)
                .collection("state").document("practice")
                .set(state).await()
        }
    }
}
