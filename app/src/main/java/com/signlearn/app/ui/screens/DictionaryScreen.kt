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
import com.signlearn.app.data.firebase.ImageRepository
import com.signlearn.app.data.model.SignVideo
import com.signlearn.app.data.model.SignImage
import com.signlearn.app.ui.components.ExoLoopingVideoPlayer
import com.signlearn.app.ui.theme.OnPrimary
import com.signlearn.app.ui.theme.Primary
import kotlinx.coroutines.launch
import com.signlearn.app.ui.util.sanitizeTitle
import com.signlearn.app.ui.util.formatCategory
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(onNavigateBack: () -> Unit) {
    val videoRepo = remember { VideoRepository() }
    val imageRepo = remember { ImageRepository() }
    val scope = rememberCoroutineScope()

    var selected by remember { mutableStateOf<DictionaryItem?>(null) }
    var mediaUrl by remember { mutableStateOf<Uri?>(null) }
    var loadingMedia by remember { mutableStateOf(false) }
    // Cache para evitar pedir repetidamente la URL de descarga y reducir jank
    val urlCache = remember { mutableStateMapOf<String, Uri>() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var reloadKey by remember { mutableStateOf(0) }
    var viewMode by remember { mutableStateOf("grid") } // "grid" | "list"
    val debugBorders = false

    // Carga de videos e imágenes
    val videosState = produceState(initialValue = emptyList<SignVideo>(), key1 = reloadKey) {
        Log.d("DictionaryScreen", "Fetching videos (reloadKey=$reloadKey)...")
        value = runCatching { videoRepo.listVideos() }
            .onFailure { Log.e("DictionaryScreen", "Error fetch videos", it) }
            .getOrDefault(emptyList())
        Log.d("DictionaryScreen", "Videos loaded: ${value.size}")
    }
    val imagesState = produceState(initialValue = emptyList<SignImage>(), key1 = reloadKey) {
        Log.d("DictionaryScreen", "Fetching images (reloadKey=$reloadKey)...")
        val fetch = runCatching { imageRepo.listImages() }
        fetch.onFailure { Log.e("DictionaryScreen", "Error fetch images", it) }
        value = fetch.getOrDefault(emptyList())
        if (value.isEmpty()) {
            Log.w("DictionaryScreen", "Images list empty. Verifica reglas Firestore o colección 'images'.")
        } else {
            Log.d("DictionaryScreen", "Images loaded: ${value.size}")
        }
    }
    val videos = videosState.value
    val images = imagesState.value
    val items = remember(videos, images) {
        val vids = videos.map { v ->
            DictionaryItem(
                id = v.id,
                title = v.title.ifBlank { v.id },
                category = v.category,
                storagePath = v.storagePath,
                type = "video"
            )
        }
        val imgs = images.map { i ->
            DictionaryItem(
                id = i.id,
                title = i.title.ifBlank { i.id },
                category = i.category,
                storagePath = i.storagePath,
                type = "image"
            )
        }
        (vids + imgs).sortedBy { it.title.lowercase() }
    }
    val categories = remember(items) { listOf("Todas") + items.map { it.category }.distinct().sorted() }

    val filteredItems = remember(items, selectedCategory, searchQuery) {
        val base = if (selectedCategory == "Todas") items else items.filter { it.category == selectedCategory }
        val result = if (searchQuery.isBlank()) base else base.filter {
            val q = searchQuery.lowercase()
            (it.title.lowercase().contains(q) || it.id.lowercase().contains(q))
        }
        Log.d("DictionaryScreen", "Filtered items count=${result.size} (query='$searchQuery' cat='$selectedCategory')")
        result
    }
    val isLoading = videos.isEmpty() && images.isEmpty()

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
                    isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    filteredItems.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay señas para mostrar") }
                    else -> if (viewMode == "grid") {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                DictionaryCard(item, debugBorders) {
                                    selected = item
                                    loadingMedia = true
                                    mediaUrl = null
                                    scope.launch {
                                        mediaUrl = urlCache[item.storagePath] ?: run {
                                            val fetch = runCatching { videoRepo.getDownloadUrl(item.storagePath) }
                                            fetch.onFailure { Log.e("DictionaryScreen", "Download URL fail", it) }
                                            val uri = fetch.getOrNull()
                                            if (uri != null) urlCache[item.storagePath] = uri
                                            uri
                                        }
                                        loadingMedia = false
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            lazyItems(filteredItems, key = { it.id }) { item ->
                                DictionaryCard(item, debugBorders) {
                                    selected = item
                                    loadingMedia = true
                                    mediaUrl = null
                                    scope.launch {
                                        mediaUrl = urlCache[item.storagePath] ?: run {
                                            val fetch = runCatching { videoRepo.getDownloadUrl(item.storagePath) }
                                            fetch.onFailure { Log.e("DictionaryScreen", "Download URL fail", it) }
                                            val uri = fetch.getOrNull()
                                            if (uri != null) urlCache[item.storagePath] = uri
                                            uri
                                        }
                                        loadingMedia = false
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
                title = { Text(sanitizeTitle(selected?.title ?: "")) },
                text = {
                    if (loadingMedia) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        mediaUrl?.let { uri ->
                            if (selected?.type == "video") {
                                ExoLoopingVideoPlayer(
                                    uri = uri,
                                    modifier = Modifier.fillMaxWidth().height(220.dp),
                                    autoPlay = true,
                                    loop = true,
                                    useController = true
                                )
                            } else {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = selected?.title,
                                    modifier = Modifier.fillMaxWidth().height(260.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } ?: Text(if (selected?.type == "video") "Video no disponible" else "Imagen no disponible")
                    }
                }
            )
        }
    }
}

@Composable
private fun DictionaryCard(item: DictionaryItem, debugBorders: Boolean, onClick: () -> Unit) {
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
                sanitizeTitle(item.title.ifBlank { item.id }),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                formatCategory(item.category),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            AssistChip(onClick = onClick, label = { Text(if (item.type == "video") "Video" else "Imagen") })
        }
    }
}

private data class DictionaryItem(
    val id: String,
    val title: String,
    val category: String,
    val storagePath: String,
    val type: String // "video" | "image"
)
