package com.internetarchive.waybackmachine.activity

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.internetarchive.waybackmachine.R
import com.jarvanmo.exoplayerview.media.SimpleMediaSource
import kotlinx.android.synthetic.main.activity_video_preview.*

class VideoPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_preview)

        val resourcePath = intent.getStringExtra("video")
        val resourceURI = Uri.parse(resourcePath)

        if (resourceURI != null) {
            videoView.play(SimpleMediaSource(resourceURI))
        }
    }

    override fun onPause() {
        videoView.releasePlayer()

        super.onPause()
    }
}
