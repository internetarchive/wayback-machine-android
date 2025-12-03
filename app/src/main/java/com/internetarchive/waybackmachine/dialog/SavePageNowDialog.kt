package com.internetarchive.waybackmachine.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.WebpageActivity
import com.internetarchive.waybackmachine.global.APIManager
import com.internetarchive.waybackmachine.global.AppManager

class SavePageNowDialog(
    context: Context,
    private val url: String,
    private val loggedInSig: String,
    private val loggedInUser: String,
    private val s3AccessKey: String,
    private val s3SecretKey: String
) : Dialog(context) {

    // View references
    private lateinit var titleTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusMessage: TextView
    private lateinit var btnViewSnapshot: android.widget.Button
    private lateinit var errorMessage: TextView
    private lateinit var btnClose: ImageView
    private var waybackUrl: String? = null

    private var jobId: String? = null
    private var isCompleted = false
    private val handler = Handler(Looper.getMainLooper())
    private var statusCheckRunnable: Runnable? = null
    private var titleAnimationRunnable: Runnable? = null
    private var pollCount = 0
    private val MAX_POLL_ATTEMPTS = 60 // Maximum 3 minutes (60 * 3 seconds)
    private var dotCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_save_page_now)

        // Set window properties to dim background
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setDimAmount(0.7f)
        
        // Make dialog size adapt to content - responsive width (80-90% of screen, max 450dp)
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val maxWidth = (screenWidth * 0.9).toInt().coerceAtMost((450 * displayMetrics.density).toInt())
        val minWidth = (280 * displayMetrics.density).toInt()
        val dialogWidth = maxWidth.coerceAtLeast(minWidth)
        
        window?.setLayout(
            dialogWidth,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Make dialog non-dismissible during save
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        // Initialize views
        titleTextView = findViewById(R.id.title)
        progressBar = findViewById(R.id.progressBar)
        statusMessage = findViewById(R.id.statusMessage)
        btnViewSnapshot = findViewById(R.id.btnViewSnapshot)
        errorMessage = findViewById(R.id.errorMessage)
        btnClose = findViewById(R.id.btnClose)

        // Set initial status
        startTitleAnimation() // Start animated "Saving page..." title
        statusMessage.text = context.getString(R.string.save_page_now_status_initializing)
        btnClose.visibility = View.INVISIBLE
        btnClose.isClickable = false
        btnClose.isFocusable = false
        btnViewSnapshot.visibility = View.INVISIBLE
        btnViewSnapshot.isClickable = false
        btnViewSnapshot.isFocusable = false

        // Close button click listener
        btnClose.setOnClickListener {
            dismiss()
        }
        
        // View snapshot button click listener
        btnViewSnapshot.setOnClickListener {
            waybackUrl?.let { url ->
                val intent = Intent(context, WebpageActivity::class.java)
                intent.putExtra("URL", url)
                context.startActivity(intent)
                dismiss()
            }
        }

        // Start the save process
        startSaveProcess()
    }

    private fun startSaveProcess() {
        android.util.Log.d("SavePageNowDialog", "=== startSaveProcess ===")
        android.util.Log.d("SavePageNowDialog", "URL: $url")
        
        // Update status
        statusMessage.text = context.getString(R.string.save_page_now_status_saving)

        // Request capture
        APIManager.getInstance(context).requestCapture(url, loggedInSig, loggedInUser, s3AccessKey, s3SecretKey) { jobId, message ->
            android.util.Log.d("SavePageNowDialog", "requestCapture callback: jobId=$jobId, message=$message")
            handler.post {
                if (!isShowing) {
                    android.util.Log.w("SavePageNowDialog", "Dialog not showing, ignoring callback")
                    return@post
                }
                
                if (jobId != null) {
                    android.util.Log.d("SavePageNowDialog", "Job ID received: $jobId")
                    this.jobId = jobId
                    
                    // Check if message indicates same snapshot was already made
                    val isSameSnapshot = !message.isNullOrEmpty() && 
                        (message.contains("same snapshot", ignoreCase = true) || 
                         message.contains("already been made", ignoreCase = true))
                    
                    if (isSameSnapshot) {
                        // Same snapshot already exists - show "Recently Saved" title and make dismissible
                        // Don't poll for status since the snapshot already exists
                        android.util.Log.d("SavePageNowDialog", "Same snapshot detected, not polling for status")
                        isCompleted = true
                        stopTitleAnimation() // Stop animated "Saving page..." dots
                        progressBar.visibility = View.GONE
                        titleTextView.text = context.getString(R.string.save_page_now_dialog_title_recently_saved)
                        // Show the message in the statusMessage area (informational message)
                        // The message explains that the snapshot was already made recently
                        statusMessage.text = message ?: context.getString(R.string.save_page_now_status_success)
                        btnClose.visibility = View.VISIBLE
                        btnClose.isClickable = true
                        btnClose.isFocusable = true
                        
                        // Make dialog dismissible
                        setCancelable(true)
                        setCanceledOnTouchOutside(true)
                    } else {
                        // Normal flow - show message if present, then start polling
                        if (!message.isNullOrEmpty()) {
                            statusMessage.text = message
                        } else {
                            statusMessage.text = context.getString(R.string.save_page_now_status_pending)
                        }
                        
                        // Start polling for status
                        startStatusPolling()
                    }
                } else {
                    // Failed to get job ID - check system status for more info
                    android.util.Log.e("SavePageNowDialog", "Failed to get job ID, checking system status")
                    checkSystemStatusAndShowError(message)
                }
            }
        }
    }

    private fun startStatusPolling() {
        pollCount = 0
        statusCheckRunnable = object : Runnable {
            override fun run() {
                // Check if we should stop polling
                if (isCompleted || jobId == null || !isShowing) {
                    return
                }

                // Check maximum poll attempts
                if (pollCount >= MAX_POLL_ATTEMPTS) {
                    handler.post {
                        if (!isCompleted && isShowing) {
                            showError("Save request is taking longer than expected. Please check back later.")
                        }
                    }
                    return
                }

                pollCount++

                APIManager.getInstance(context).requestCaptureStatus(
                    jobId!!,
                    loggedInSig,
                    loggedInUser,
                    s3AccessKey,
                    s3SecretKey
                ) { result, error ->
                    handler.post {
                        if (isCompleted || !isShowing) {
                            return@post
                        }

                        if (error != null) {
                            // Error occurred - check system status for more info
                            android.util.Log.e("SavePageNowDialog", "Status check error: $error")
                            checkSystemStatusAndShowError(error)
                        } else if (result != null) {
                            if (result == "pending") {
                                // Still pending, poll again after 3 seconds
                                android.util.Log.d("SavePageNowDialog", "Status is pending (poll count: $pollCount)")
                                handler.postDelayed(this, 3000)
                            } else {
                                // Success - result contains the wayback URL
                                android.util.Log.d("SavePageNowDialog", "Status check success: $result")
                                showSuccess(result)
                            }
                        } else {
                            // Unknown error - check system status
                            android.util.Log.e("SavePageNowDialog", "Unknown error: result and error both null")
                            checkSystemStatusAndShowError(null)
                        }
                    }
                }
            }
        }

        // Start polling after 3 seconds
        handler.postDelayed(statusCheckRunnable!!, 3000)
    }

    private fun showSuccess(waybackUrl: String) {
        isCompleted = true
        stopTitleAnimation() // Stop animated dots
        this.waybackUrl = waybackUrl
        progressBar.visibility = View.GONE
        titleTextView.text = context.getString(R.string.save_page_now_dialog_title_succeeded)
        statusMessage.text = context.getString(R.string.save_page_now_status_success)
        btnViewSnapshot.visibility = View.VISIBLE
        btnViewSnapshot.isClickable = true
        btnViewSnapshot.isFocusable = true
        btnClose.visibility = View.VISIBLE
        btnClose.isClickable = true
        btnClose.isFocusable = true

        // Make dialog dismissible
        setCancelable(true)
        setCanceledOnTouchOutside(true)
    }

    private fun checkSystemStatusAndShowError(apiMessage: String?) {
        // Check system status first to get more detailed error info
        APIManager.getInstance(context).checkSystemStatus(loggedInSig, loggedInUser) { systemError ->
            handler.post {
                if (!isShowing) {
                    android.util.Log.w("SavePageNowDialog", "Dialog not showing, ignoring system status callback")
                    return@post
                }
                
                // Prioritize system error message, then API message, then generic error
                val errorMsg = when {
                    !systemError.isNullOrEmpty() -> {
                        android.util.Log.d("SavePageNowDialog", "Using system status error: $systemError")
                        systemError
                    }
                    !apiMessage.isNullOrEmpty() -> {
                        android.util.Log.d("SavePageNowDialog", "Using API error message: $apiMessage")
                        apiMessage
                    }
                    else -> {
                        android.util.Log.d("SavePageNowDialog", "Using generic error message")
                        "Failed to initiate save request. Please check:\n1. Your internet connection\n2. You are logged in (Login required for Save Page Now)\n3. Try again in a few moments"
                    }
                }
                
                showError(errorMsg)
            }
        }
    }
    
    private fun showError(error: String) {
        isCompleted = true
        stopTitleAnimation() // Stop animated dots
        progressBar.visibility = View.GONE
        titleTextView.text = context.getString(R.string.save_page_now_dialog_title_failed)
        statusMessage.text = context.getString(R.string.save_page_now_status_error)
        errorMessage.text = error
        errorMessage.visibility = View.VISIBLE
        btnClose.visibility = View.VISIBLE
        btnClose.isClickable = true
        btnClose.isFocusable = true

        // Make dialog dismissible
        setCancelable(true)
        setCanceledOnTouchOutside(true)
    }

    private fun startTitleAnimation() {
        stopTitleAnimation() // Stop any existing animation
        
        val baseTitle = context.getString(R.string.save_page_now_dialog_title_saving)
        titleAnimationRunnable = object : Runnable {
            override fun run() {
                if (isCompleted || !isShowing) {
                    return
                }
                
                // Cycle through: "Saving page", "Saving page.", "Saving page..", "Saving page..."
                val dots = ".".repeat(dotCount % 4)
                titleTextView.text = "$baseTitle$dots"
                dotCount++
                
                // Continue animation every 500ms
                handler.postDelayed(this, 500)
            }
        }
        
        // Start animation
        handler.post(titleAnimationRunnable!!)
    }
    
    private fun stopTitleAnimation() {
        titleAnimationRunnable?.let {
            handler.removeCallbacks(it)
            titleAnimationRunnable = null
        }
    }

    override fun dismiss() {
        // Mark as completed to stop any ongoing operations
        isCompleted = true
        
        // Stop title animation
        stopTitleAnimation()
        
        // Cancel any pending status checks
        statusCheckRunnable?.let {
            handler.removeCallbacks(it)
            statusCheckRunnable = null
        }
        super.dismiss()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Mark as completed to stop any ongoing operations
        isCompleted = true
        
        // Stop title animation
        stopTitleAnimation()
        
        // Clean up
        statusCheckRunnable?.let {
            handler.removeCallbacks(it)
            statusCheckRunnable = null
        }
    }
    
    init {
        // Set up cancel listener to handle cleanup
        setOnCancelListener {
            isCompleted = true
            stopTitleAnimation()
            statusCheckRunnable?.let {
                handler.removeCallbacks(it)
                statusCheckRunnable = null
            }
        }
    }
}

