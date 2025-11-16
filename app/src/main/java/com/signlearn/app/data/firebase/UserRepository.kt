package com.signlearn.app.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.signlearn.app.data.model.UserProgress
import com.signlearn.app.data.model.UserProfile
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    suspend fun addXP(uid: String, amount: Int) {
        val doc = db.collection("users").document(uid)
        doc.set(mapOf("totalPoints" to FieldValue.increment(amount.toLong())), com.google.firebase.firestore.SetOptions.merge()).await()
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
            val profile = UserProfile(uid = uid, displayName = displayName, email = email, totalPoints = 0)
            userRef.set(profile).await()
        } else {
            // merge minimal fields in case values are empty
            userRef.set(
                mapOf(
                    "displayName" to (displayName ?: snap.getString("displayName")),
                    "email" to (email ?: snap.getString("email"))
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val snap = db.collection("users").document(uid).get().await()
        return snap.toObject(UserProfile::class.java)
    }

    suspend fun getCompletedLessonsCount(uid: String): Int {
        val snap = db.collection("users").document(uid).collection("progress").get().await()
        return snap.size()
    }
}
