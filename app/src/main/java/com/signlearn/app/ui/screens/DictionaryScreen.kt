package com.signlearn.app.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.signlearn.app.data.firebase.VideoRepository
import com.signlearn.app.data.model.SignVideo
import com.signlearn.app.ui.components.ExoLoopingVideoPlayer
import com.signlearn.app.ui.theme.OnPrimary
import com.signlearn.app.ui.theme.Primary
import kotlinx.coroutines.launch
import com.signlearn.app.ui.util.sanitizeTitle
import com.signlearn.app.ui.util.formatCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(onNavigateBack: () -> Unit) {
    val repo = remember { VideoRepository() }
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<SignVideo?>(null) }
    var videoUrl by remember { mutableStateOf<Uri?>(null) }
    var loadingVideo by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var reloadKey by remember { mutableStateOf(0) }
    var viewMode by remember { mutableStateOf("grid") } // "grid" | "list"
    val debugBorders = false

    // Carga de videos
    val videosState = produceState(initialValue = emptyList<SignVideo>(), key1 = reloadKey) {
        Log.d("DictionaryScreen", "Fetching videos (reloadKey=$reloadKey)...")
        value = runCatching { repo.listVideos() }
            .onFailure { Log.e("DictionaryScreen", "Error fetch videos", it) }
            .getOrDefault(emptyList())
        Log.d("DictionaryScreen", "Videos loaded: ${value.size}")
        value.take(3).forEach { Log.d("DictionaryScreen", "Sample -> id=${it.id} title=${it.title} cat=${it.category} path=${it.storagePath}") }
    }
    val videos = videosState.value
    val categories = remember(videos) { listOf("Todas") + videos.map { it.category }.distinct().sorted() }

    val filteredVideos = remember(videos, selectedCategory, searchQuery) {
        val base = if (selectedCategory == "Todas") videos else videos.filter { it.category == selectedCategory }
        val result = if (searchQuery.isBlank()) base else base.filter {
            val q = searchQuery.lowercase()
            (it.title.lowercase().contains(q) || it.id.lowercase().contains(q))
        }
        Log.d("DictionaryScreen", "Filtered videos count=${result.size} (query='$searchQuery' cat='$selectedCategory')")
        result
    }
    val isLoadingVideos = videos.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diccionario") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = OnPrimary, navigationIconContentColor = OnPrimary)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // Restaurar fondo con gradiente suave en lugar del color debug oscuro
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Buscar seña") },
                    singleLine = true
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { reloadKey++ }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Recargar")
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { viewMode = if (viewMode == "grid") "list" else "grid" }) {
                    if (viewMode == "grid") Icon(Icons.Filled.List, contentDescription = "Lista") else Icon(Icons.Filled.GridView, contentDescription = "Grilla")
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(categories.size) { idx ->
                    val cat = categories[idx]
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(if (cat == "Todas") cat else formatCategory(cat)) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Área de resultados: usar weight(1f) para ocupar solo el espacio restante y no empujar las tarjetas fuera de la pantalla.
            // Banner de conteo
            // (Banner total removido según solicitud)
            // Lista única (LazyColumn) que evita el problema de constraints infinitos.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    isLoadingVideos -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    filteredVideos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay señas para mostrar") }
                    else -> if (viewMode == "grid") {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredVideos, key = { it.id }) { v ->
                                VideoDictionaryCard(v, debugBorders) {
                                    selected = v
                                    loadingVideo = true
                                    videoUrl = null
                                    scope.launch {
                                        runCatching { repo.getDownloadUrl(v.storagePath) }
                                            .onSuccess { videoUrl = it }
                                            .onFailure { Log.e("DictionaryScreen", "Download URL fail", it) }
                                            .also { loadingVideo = false }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            lazyItems(filteredVideos, key = { it.id }) { v ->
                                VideoDictionaryCard(v, debugBorders) {
                                    selected = v
                                    loadingVideo = true
                                    videoUrl = null
                                    scope.launch {
                                        runCatching { repo.getDownloadUrl(v.storagePath) }
                                            .onSuccess { videoUrl = it }
                                            .onFailure { Log.e("DictionaryScreen", "Download URL fail", it) }
                                            .also { loadingVideo = false }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (selected != null) {
            AlertDialog(
                onDismissRequest = { selected = null },
                confirmButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } },
                title = { Text(sanitizeTitle(selected?.title)) },
                text = {
                    if (loadingVideo) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        videoUrl?.let { uri ->
                            ExoLoopingVideoPlayer(
                                uri = uri,
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                autoPlay = true,
                                loop = true,
                                useController = true
                            )
                        } ?: Text("Video no disponible")
                    }
                }
            )
        }
    }
}

@Composable
private fun VideoDictionaryCard(video: SignVideo, debugBorders: Boolean, onClick: () -> Unit) {
    val borderModifier = if (debugBorders) Modifier.border(1.dp, MaterialTheme.colorScheme.primary) else Modifier
    Card(
        modifier = Modifier
            .height(140.dp)
            .fillMaxWidth()
            .then(borderModifier)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                sanitizeTitle(video.title.ifBlank { video.id }),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatCategory(video.category),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
