package com.signlearn.app.data.firebase

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.storage
import com.signlearn.app.data.model.SignVideo
import kotlinx.coroutines.tasks.await

class VideoRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun listVideos(): List<SignVideo> {
        val snap: QuerySnapshot = db.collection("videos").get().await()
        return snap.documents.mapNotNull { doc ->
            doc.toObject(SignVideo::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun getDownloadUrl(storagePath: String): Uri {
        val storage = Firebase.storage
        return storage.reference.child(storagePath).downloadUrl.await()
    }
}
