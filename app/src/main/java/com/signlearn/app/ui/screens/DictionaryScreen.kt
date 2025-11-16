package com.signlearn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.signlearn.app.ui.theme.*
import com.signlearn.app.data.model.SignVideo
import com.signlearn.app.data.firebase.VideoRepository
import com.signlearn.app.ui.components.VideoPlayer
import kotlinx.coroutines.launch
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onNavigateBack: () -> Unit,
    videos: List<SignVideo>
) {
    val scope = rememberCoroutineScope()
    val repo = remember { VideoRepository() }
    var selected by remember { mutableStateOf<SignVideo?>(null) }
    var videoUrl by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diccionario") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = OnPrimary, navigationIconContentColor = OnPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(brush = Brush.verticalGradient(listOf(Primary.copy(alpha = 0.05f), MaterialTheme.colorScheme.background)))
                .padding(16.dp)
        ) {
            LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(videos) { v ->
                    Card(onClick = {
                        selected = v
                        loading = true
                        videoUrl = null
                        scope.launch {
                            runCatching { repo.getDownloadUrl(v.storagePath) }
                                .onSuccess { uri -> videoUrl = uri }
                                .onFailure { /* TODO: mostrar error si se desea */ }
                                .also { loading = false }
                        }
                    }) {
                        Box(Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(v.title.ifBlank { v.id }, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            if (selected != null) {
                AlertDialog(
                    onDismissRequest = { selected = null },
                    confirmButton = {
                        TextButton(onClick = { selected = null }) { Text("Cerrar") }
                    },
                    title = { Text(selected?.title ?: "") },
                    text = {
                        if (loading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            videoUrl?.let { uri ->
                                VideoPlayer(uri = uri, modifier = Modifier.fillMaxWidth().height(220.dp))
                            } ?: Text("Video no disponible")
                        }
                    }
                )
            }
        }
    }
}
