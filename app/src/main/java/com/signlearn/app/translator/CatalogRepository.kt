package com.signlearn.app.translator

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CatalogRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // Espera una colección 'catalog' con documentos { label: string, word: string, translation: string }
    suspend fun getLabelMap(): Map<String, CatalogEntry> {
        val snap = db.collection("catalog").get().await()
        val map = mutableMapOf<String, CatalogEntry>()
        for (doc in snap.documents) {
            val label = doc.getString("label") ?: continue
            val word = doc.getString("word") ?: label
            val translation = doc.getString("translation") ?: word
            map[label] = CatalogEntry(label = label, word = word, translation = translation)
        }
        return map
    }

    data class CatalogEntry(val label: String, val word: String, val translation: String)
}
