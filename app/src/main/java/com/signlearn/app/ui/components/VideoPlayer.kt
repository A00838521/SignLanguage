package com.signlearn.app.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoPlayer(uri: Uri, modifier: Modifier = Modifier, autoPlay: Boolean = true, loop: Boolean = false) {
    AndroidView(factory = { context ->
        VideoView(context).apply {
            val controller = MediaController(context)
            controller.setAnchorView(this)
            setMediaController(controller)
            setVideoURI(uri)
            tag = uri
            setOnPreparedListener { mp ->
                mp.isLooping = loop
                if (autoPlay) start()
            }
        }
    }, update = { view ->
        val last = view.tag as? Uri
        if (last != uri) {
            view.setVideoURI(uri)
            view.tag = uri
            if (autoPlay) view.start()
        }
    }, modifier = modifier)
}
