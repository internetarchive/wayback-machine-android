package com.archive.waybackmachine.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.archive.waybackmachine.R
import com.archive.waybackmachine.dialog.ShareIntentDialog
import com.archive.waybackmachine.global.AppManager
import kotlinx.android.synthetic.main.activity_webpage.*

class WebpageActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webpage)

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
        val dialogShare = ShareIntentDialog.Builder(this)
                .setDialogTitle("Share with your friends")
                .setShareLink(webView.url?: "")
                .build()

        dialogShare.show()
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
