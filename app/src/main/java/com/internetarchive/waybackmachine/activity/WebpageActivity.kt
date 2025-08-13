package com.internetarchive.waybackmachine.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.global.AppManager
import android.widget.TextView
import android.widget.ImageView
import android.widget.FrameLayout

class WebpageActivity : AppCompatActivity(), View.OnClickListener {

    // View references
    private lateinit var btnBack: TextView
    private lateinit var btnOpen: ImageView
    private lateinit var btnShare: ImageView
    private lateinit var webView: WebView
    private lateinit var containerIndicator: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webpage)

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        btnOpen = findViewById(R.id.btnOpen)
        btnShare = findViewById(R.id.btnShare)
        webView = findViewById(R.id.webView)
        containerIndicator = findViewById(R.id.containerIndicator)

        val url = intent.getStringExtra("URL")

        btnBack.setOnClickListener(this)
        btnOpen.setOnClickListener(this)
        btnShare.setOnClickListener(this)

        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        webView.isVerticalScrollBarEnabled = true
        webView.settings.setSupportZoom(true)
        webView.settings.useWideViewPort = true

        val webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                containerIndicator.visibility = View.INVISIBLE
            }
        }

        val header = mutableMapOf<String, String>()
        header["User-Agent"] = "Wayback_Machine_Android/" + AppManager.getInstance(this).getVersionName()
        webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"
        webView.webViewClient = webViewClient
        webView.loadUrl(url?: "", header)
        containerIndicator.visibility = View.VISIBLE
    }

    private fun onBack() {
        finish()
    }

    private fun onOpen() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webView.url))
        startActivity(browserIntent)
    }

    private fun onShare() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, webView.url ?: "")

        val chooserIntent = Intent.createChooser(shareIntent, "Share with your friends")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (chooserIntent.resolveActivity(packageManager) != null) {
            startActivity(chooserIntent)
        }
    }

    override fun onClick(v: View?) {
        if (v == null) return

        when (v.id) {
            R.id.btnBack -> {
                onBack()
            }
            R.id.btnOpen -> {
                onOpen()
            }
            R.id.btnShare -> {
                onShare()
            }
        }
    }
}
