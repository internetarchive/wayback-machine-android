package com.internetarchive.waybackmachine.activity

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.internetarchive.waybackmachine.R
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem

class VideoPreviewActivity : AppCompatActivity() {

    private lateinit var videoView: PlayerView
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_preview)

        // Initialize view
        videoView = findViewById(R.id.videoView)

        val resourcePath = intent.getStringExtra("video")
        val resourceURI = Uri.parse(resourcePath)

        if (resourceURI != null) {
            // Create and set up ExoPlayer
            player = ExoPlayer.Builder(this).build()
            videoView.player = player
            
            // Create media item and play
            val mediaItem = MediaItem.fromUri(resourceURI)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
        }
    }

    override fun onPause() {
        player?.release()
        player = null
        super.onPause()
    }
}
