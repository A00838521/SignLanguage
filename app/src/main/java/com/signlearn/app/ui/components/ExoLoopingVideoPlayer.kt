package com.signlearn.app.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun ExoLoopingVideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    loop: Boolean = true,
    useController: Boolean = false
) {
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    val context = LocalContext.current

    DisposableEffect(uri) {
        val exo = ExoPlayer.Builder(context).build().apply {
            val item = MediaItem.fromUri(uri)
            setMediaItem(item)
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            prepare()
            if (autoPlay) playWhenReady = true
        }
        player = exo
        onDispose {
            exo.stop()
            exo.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                this.useController = useController
            }
        },
        update = { view ->
            view.player = player
            view.useController = useController
        },
        modifier = modifier
    )
}
