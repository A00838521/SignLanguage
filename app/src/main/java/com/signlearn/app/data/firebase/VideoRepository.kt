package com.signlearn.app.data.firebase

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
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
        // Asegurar uso del bucket correcto incluso si google-services.json no está en app/
        val bucket = FirebaseApp.getInstance().options.storageBucket
        val storage = if (!bucket.isNullOrBlank()) {
            FirebaseStorage.getInstance("gs://$bucket")
        } else {
            FirebaseStorage.getInstance()
        }
        return storage.reference.child(storagePath).downloadUrl.await()
    }
}
