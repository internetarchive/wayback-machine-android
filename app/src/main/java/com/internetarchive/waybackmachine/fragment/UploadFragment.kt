package com.internetarchive.waybackmachine.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.media3.ui.PlayerView
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.features.ReturnMode
import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.MainActivity
import com.internetarchive.waybackmachine.activity.PhotoPreviewActivity
import com.internetarchive.waybackmachine.activity.VideoPreviewActivity
import com.internetarchive.waybackmachine.global.AppManager
import androidx.appcompat.app.AlertDialog
import java.util.Locale
import java.io.File
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import android.app.Activity

class UploadFragment : Fragment(), View.OnClickListener {
    private var mainActivity: MainActivity? = null
    private var resourcePath: String? = null
    private var fileExt: String? = null
    private var mediaType: String? = null
    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var mContext: Context

    // View references
    private lateinit var btnAttach: Button
    private lateinit var btnUpload: Button
    private lateinit var imageView: ImageView
    private lateinit var videoView: PlayerView
    private lateinit var txtTitle: android.widget.EditText
    private lateinit var txtDescription: android.widget.EditText
    private lateinit var txtSubject: android.widget.EditText

    // Modern permission launcher
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    
    // Modern activity result launcher for image picker
    private var imagePickerLauncher: ActivityResultLauncher<Intent>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted, proceed with image picker
                android.util.Log.d("UploadFragment", "Permission granted, launching image picker")
                launchImagePicker()
            } else {
                // Permission denied
                android.util.Log.w("UploadFragment", "Permission denied by user")
                mainActivity?.let { activity ->
                    AlertDialog.Builder(activity)
                        .setTitle("Permission Required")
                        .setMessage("Storage permission is required to select photos and videos. Please grant the permission in Settings.")
                        .setPositiveButton("Go to Settings") { _, _ ->
                            // Open app settings
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = android.net.Uri.fromParts("package", activity.packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel") { _, _ -> }
                        .show()
                }
            }
        }
        
        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (ImagePicker.shouldHandle(100, result.resultCode, data)) {
                    val image = ImagePicker.getFirstImageOrNull(data)
                    val resourceURI = android.net.Uri.parse(image.path)
                    resourcePath = image.path

                    if (resourcePath != null) {
                        fileExt = resourcePath!!.substring(resourcePath!!.lastIndexOf(".")).lowercase()
                        
                        // Release any existing player
                        videoView.player?.release()

                        if (fileExt == ".mp4" || fileExt == ".3gp" || fileExt == ".mpg") {
                            videoView.visibility = View.VISIBLE
                            imageView.visibility = View.INVISIBLE
                            mediaType = "video"
                            
                            // Create and set up ExoPlayer
                            val player = androidx.media3.exoplayer.ExoPlayer.Builder(requireContext()).build()
                            videoView.player = player
                            
                            // Set media item and prepare
                            val mediaItem = androidx.media3.common.MediaItem.fromUri(resourceURI)
                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.playWhenReady = false
                        } else {
                            videoView.visibility = View.INVISIBLE
                            imageView.visibility = View.VISIBLE
                            imageView.setImageURI(resourceURI)
                            mediaType = "image"
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_upload, container, false)
        
        // Initialize views
        btnAttach = view.findViewById(R.id.btnAttach)
        btnUpload = view.findViewById(R.id.btnUpload)
        imageView = view.findViewById(R.id.imageView)
        videoView = view.findViewById(R.id.videoView)
        txtTitle = view.findViewById(R.id.txtTitle)
        txtDescription = view.findViewById(R.id.txtDescription)
        txtSubject = view.findViewById(R.id.txtSubject)
        
        btnAttach.setOnClickListener(this)
        btnUpload.setOnClickListener(this)
        imageView.setOnClickListener(this)
        videoView.setOnClickListener(this)
        videoView.visibility = View.INVISIBLE
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            if (context is MainActivity) {
                mainActivity = context
                mContext = context
            } else {
                android.util.Log.w("UploadFragment", "Context is not MainActivity: ${context.javaClass.simpleName}")
            }
        } catch (e: Exception) {
            android.util.Log.e("UploadFragment", "Error in onAttach", e)
        }
    }

    override fun onClick(v: View?) {
        if (v == null) return

        when (v.id) {
            R.id.btnAttach -> {
                onAttachMedia()
            }
            R.id.btnUpload -> {
                onUpload()
            }
            R.id.imageView -> {
                if (resourcePath == null) return

                val intent = Intent(requireActivity(), PhotoPreviewActivity::class.java)
                intent.putStringArrayListExtra("photos", arrayListOf(resourcePath))

                startActivity(intent)
            }
            R.id.videoView -> {
                if (resourcePath == null) return

                val intent = Intent(requireActivity(), VideoPreviewActivity::class.java)
                intent.putExtra("video", resourcePath)

                startActivity(intent)
            }
        }
    }

    private fun onAttachMedia() {
        // Check if user is logged in first
        val userInfo = AppManager.getInstance(mainActivity).userInfo
        
        if (userInfo == null) {
            mainActivity?.let { activity ->
                AlertDialog.Builder(activity)
                    .setTitle("Login is required")
                    .setMessage("You need to login to upload photo or video")
                    .setPositiveButton("OK") { _, _ ->
                        mainActivity?.let { main ->
                            main.replaceSigninFragment()
                        }
                    }
                    .show()
            }
            return
        }
        
        // Check if launchers are initialized
        if (permissionLauncher == null || imagePickerLauncher == null) {
            android.util.Log.w("UploadFragment", "Launchers not initialized yet, skipping operation")
            return
        }
        
        // Check permission first - handle both old and new Android versions
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(mContext, permission) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, launch image picker
            launchImagePicker()
        } else {
            // Request permission
            permissionLauncher?.launch(permission)
        }
    }
    
    private fun launchImagePicker() {
        try {
            val intent = ImagePicker.create(this)
                .returnMode(ReturnMode.ALL)
                .folderMode(true)
                .toolbarFolderTitle("Folder")
                .toolbarImageTitle("Tap to select")
                .toolbarArrowColor(android.graphics.Color.WHITE)
                .single()
                .includeVideo(true)
                .showCamera(true)
                .enableLog(false)
                .getIntent(requireContext())
            
            imagePickerLauncher?.launch(intent)
            android.util.Log.d("UploadFragment", "Image picker launched successfully")
        } catch (e: Exception) {
            android.util.Log.e("UploadFragment", "Failed to launch image picker", e)
            AppManager.getInstance(mainActivity)?.displayToast("Failed to open image picker: ${e.message}")
        }
    }

    private fun onUpload() {
        val userInfo = AppManager.getInstance(mainActivity).userInfo

        if (!validateFields()) return
        if (userInfo == null) {
            mainActivity?.let { activity ->
                AlertDialog.Builder(activity)
                    .setTitle("Login is required")
                    .setMessage("You need to login to upload photo or video")
                    .setPositiveButton("OK") { _, _ ->
                        // Use the proper navigation method instead of manipulating nav state directly
                        mainActivity?.let { main ->
                            main.replaceSigninFragment()
                        }
                    }
                    .show()
            }
            return
        }
        
        android.util.Log.d("UploadFragment", "✅ User is logged in for upload - proceeding with upload")

        val username = userInfo.username
        val identifier = username + "_" + System.currentTimeMillis() / 1000L
        val title = txtTitle.text.toString()
        val description = txtDescription.text.toString()
        val tags = txtSubject.text.toString()
        val filename = "$identifier$fileExt"
        val s3accesskey = userInfo.s3AccessKey
        val s3secretkey = userInfo.s3SecretKey
        val startTime = System.currentTimeMillis()

        mainActivity?.showProgressBar()
        val originalMap: Map<String, String?> = mapOf(
            "identifier" to identifier,
            "title" to title,
            "description" to description,
            "tags" to tags,
            "path" to resourcePath!!,
            "filename" to filename,
            "s3accesskey" to s3accesskey,
            "s3secretkey" to s3secretkey,
            "mediatype" to mediaType!!
        )

        val filteredMap: MutableMap<String, String> = mutableMapOf()
        for ((key, value) in originalMap) {
            if (value != null) {
                filteredMap[key] = value
            }
        }
        
        // Create a File object from the resource path
        val file = File(resourcePath!!)
        
        com.internetarchive.waybackmachine.global.APIManager.getInstance(mainActivity).uploadFile(
            file,
            txtTitle.text.toString(),
            txtDescription.text.toString(),
            txtSubject.text.toString()
        ) { success, err ->
            mainActivity?.runOnUiThread {
                mainActivity?.hideProgressBar()

                val endTime = System.currentTimeMillis()
                val duration = formatTime((endTime - startTime) / 1000L)

                if (success) {
                    val txtView = android.widget.TextView(context)
                    txtView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    val spannable = android.text.SpannableString(
                        "Uploaded successfully \n In $duration \n Available here " +
                                "https://archive.org/details/$identifier"
                    )
                    val iStart = spannable.toString().indexOf("https://")
                    val iEnd = spannable.toString().length
                    val clickableSpan = object : android.text.style.ClickableSpan() {
                        override fun onClick(widget: View) {
                            val intent = Intent(mainActivity, com.internetarchive.waybackmachine.activity.WebpageActivity::class.java)
                            intent.putExtra("URL", spannable.toString().substring(iStart))
                            startActivity(intent)
                        }

                        override fun updateDrawState(ds: android.text.TextPaint) {
                            super.updateDrawState(ds)
                            ds.isUnderlineText = false
                            ds.color = ContextCompat.getColor(mContext, R.color.fcBlue)
                        }
                    }
                    spannable.setSpan(clickableSpan, iStart, iEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    txtView.text = spannable
                    txtView.movementMethod = android.text.method.LinkMovementMethod.getInstance()

                    mainActivity?.let { activity ->
                        AlertDialog.Builder(activity)
                            .setTitle("Successfully Uploaded")
                            .setView(txtView)
                            .setPositiveButton("OK") { _, _ -> }
                            .show()
                    }
                } else {
                    mainActivity?.let { activity ->
                        AlertDialog.Builder(activity)
                            .setTitle("Uploading failed")
                            .setMessage(err ?: "Unknown error")
                            .setPositiveButton("OK") { _, _ -> }
                            .show()
                    }
                }
            }
        }
    }

    private fun validateFields() : Boolean {
        if (txtTitle.text.isEmpty()) {
            AppManager.getInstance(mainActivity).displayToast("Title is required")
            return false
        }
        if (txtDescription.text.isEmpty()) {
            AppManager.getInstance(mainActivity).displayToast("Description is required")
            return false
        }
        if (txtSubject.text.isEmpty()) {
            AppManager.getInstance(mainActivity).displayToast("Subject is required")
            return false
        }
        if (resourcePath == null) {
            AppManager.getInstance(mainActivity).displayToast("You need to attach photo or video")
            return false
        }

        return true
    }

    private fun formatTime(totalSeconds: Long): String {
        val minutesInAnHour = 60
        val secondsInAMinute = 60

        val seconds = totalSeconds % secondsInAMinute
        val totalMinutes = totalSeconds / secondsInAMinute
        val minutes = totalMinutes % minutesInAnHour

        val hours = totalMinutes / minutesInAnHour


        var ret = ""
        if (hours > 0) {
            ret += hours.toString() + "hrs"
        }
        if (minutes > 0) {
            ret += minutes.toString() + "mins"
        }
        if (seconds > 0) {
            ret += seconds.toString() + "secs"
        }

        return ret
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                UploadFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }

}
