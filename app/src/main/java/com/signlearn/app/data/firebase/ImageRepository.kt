package com.signlearn.app.data.firebase

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.signlearn.app.data.model.SignImage
import kotlinx.coroutines.tasks.await

class ImageRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun listImages(): List<SignImage> {
        val snap: QuerySnapshot = db.collection("images").get().await()
        return snap.documents.mapNotNull { doc ->
            doc.toObject(SignImage::class.java)?.copy(id = doc.id)
        }
    }

    suspend fun getDownloadUrl(storagePath: String): Uri {
        val bucket = FirebaseApp.getInstance().options.storageBucket
        val storage = if (!bucket.isNullOrBlank()) {
            FirebaseStorage.getInstance("gs://$bucket")
        } else {
            FirebaseStorage.getInstance()
        }
        return storage.reference.child(storagePath).downloadUrl.await()
    }
}
