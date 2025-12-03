package com.signlearn.app.translator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslatorViewModel : ViewModel() {
    data class UiState(
        val isDetecting: Boolean = false,
        val alphabetOnly: Boolean = false,
        val word: String? = null,
        val translation: String? = null,
        val confidence: Float = 0f,
        val ready: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val repo = CatalogRepository(FirebaseFirestore.getInstance())
    private var labelMap: Map<String, CatalogRepository.CatalogEntry> = emptyMap()
    private val recentLabels: ArrayDeque<String> = ArrayDeque()
    private val smoothWindow = 7

    init {
        // Cargar mapeo de catálogo desde Firestore
        viewModelScope.launch {
            runCatching {
                labelMap = withContext(Dispatchers.IO) { repo.getLabelMap() }
            }.onSuccess {
                _state.value = _state.value.copy(ready = true)
            }.onFailure {
                _state.value = _state.value.copy(ready = true)
            }
        }
    }

    fun setDetecting(detecting: Boolean) {
        _state.value = _state.value.copy(isDetecting = detecting)
    }

    fun setAlphabetOnly(enabled: Boolean) {
        _state.value = _state.value.copy(alphabetOnly = enabled)
    }

    fun publish(label: String?, confidence: Float) {
        viewModelScope.launch {
            val smoothedLabel = smooth(label)
            // En modo solo letras, aceptar solo etiquetas de una sola letra A-Z
            val filtered = smoothedLabel?.let {
                if (_state.value.alphabetOnly) {
                    if (it.length == 1 && it[0].isLetter() && it[0].isUpperCase()) it else null
                } else it
            }
            val entry = filtered?.let { labelMap[it] }
            _state.value = _state.value.copy(
                word = entry?.word ?: filtered,
                translation = entry?.translation ?: filtered,
                confidence = confidence,
            )
        }
    }

    fun clear() {
        _state.value = UiState()
    }

    private fun smooth(label: String?): String? {
        if (label == null) return null
        recentLabels.addLast(label)
        if (recentLabels.size > smoothWindow) recentLabels.removeFirst()
        // Mayoría simple en la ventana
        val counts = recentLabels.groupingBy { it }.eachCount()
        return counts.maxByOrNull { it.value }?.key
    }
}
