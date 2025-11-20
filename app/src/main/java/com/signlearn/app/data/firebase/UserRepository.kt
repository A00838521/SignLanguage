package com.signlearn.app.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.signlearn.app.data.model.UserProgress
import com.signlearn.app.data.model.UserProfile
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
}
