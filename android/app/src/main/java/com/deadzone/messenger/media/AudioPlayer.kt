package com.deadzone.messenger.media

import android.media.MediaPlayer
import java.io.File
import java.io.IOException

class AudioPlayer {
    private var player: MediaPlayer? = null
    var isPlaying = false
        private set

    fun play(file: File, onCompletion: () -> Unit) {
        if (isPlaying) stop()
        
        player = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioPlayer.isPlaying = false
                    onCompletion()
                }
                start()
                this@AudioPlayer.isPlaying = true
            } catch (e: IOException) {
                e.printStackTrace()
                this@AudioPlayer.isPlaying = false
                onCompletion()
            }
        }
    }

    fun stop() {
        player?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        player = null
        isPlaying = false
    }
}
