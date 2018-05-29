package com.archive.waybackmachine.fragment

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cn.pedant.SweetAlert.SweetAlertDialog

import com.archive.waybackmachine.R
import com.archive.waybackmachine.activity.MainActivity
import com.archive.waybackmachine.activity.PhotoPreviewActivity
import com.archive.waybackmachine.activity.VideoPreviewActivity
import com.archive.waybackmachine.activity.WebpageActivity
import com.archive.waybackmachine.global.APIManager
import com.archive.waybackmachine.global.AppManager
import com.esafirm.imagepicker.features.ImagePicker
import com.esafirm.imagepicker.features.ReturnMode
import com.jarvanmo.exoplayerview.media.SimpleMediaSource
import kotlinx.android.synthetic.main.fragment_upload.*
import kotlinx.android.synthetic.main.fragment_upload.view.*

class UploadFragment : Fragment(), View.OnClickListener {
    private var mainActivity: MainActivity? = null
    private var resourcePath: String? = null
    private var fileExt: String? = null
    private var mediaType: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_upload, container, false)
        view.btnAttach.setOnClickListener(this)
        view.btnUpload.setOnClickListener(this)
        view.imageView.setOnClickListener(this)
        view.videoView.setOnClickListener(this)
        view.videoView.visibility = View.INVISIBLE
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is MainActivity) {
            mainActivity = context

            val userInfo = AppManager.getInstance(mainActivity).userInfo
            if (userInfo == null) {
                SweetAlertDialog(mainActivity, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Login is required")
                        .setContentText("You need to login to upload photo or video")
                        .setConfirmText("OK")
                        .show()

                mainActivity?.selectMenuItem(3)
            }
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

                val intent = Intent(activity, PhotoPreviewActivity::class.java)
                intent.putStringArrayListExtra("photos", arrayListOf(resourcePath))

                startActivity(intent)
            }
            R.id.videoView -> {
                if (resourcePath == null) return

                val intent = Intent(activity, VideoPreviewActivity::class.java)
                intent.putExtra("video", resourcePath)

                startActivity(intent)
            }
        }
    }

    private fun onAttachMedia() {
        ImagePicker.create(this)
                .returnMode(ReturnMode.ALL)
                .folderMode(true)
                .toolbarFolderTitle("Folder")
                .toolbarImageTitle("Tap to select")
                .toolbarArrowColor(Color.WHITE)
                .single()
                .includeVideo(true)
                .showCamera(true)
                .enableLog(false)
                .start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            val images = ImagePicker.getImages(data)
            val image = ImagePicker.getFirstImageOrNull(data)
            val resourceURI = Uri.parse(image.path)
            resourcePath = image.path

            if (resourcePath != null) {
                fileExt = resourcePath!!.substring(resourcePath!!.lastIndexOf(".")).toLowerCase()
                videoView.releasePlayer()

                if (fileExt == ".mp4" || fileExt == ".3gp" || fileExt == ".mpg") {
                    videoView.visibility = View.VISIBLE
                    imageView.visibility = View.INVISIBLE
                    videoView.play(SimpleMediaSource(resourceURI))
                    mediaType = "video"
                } else {
                    videoView.visibility = View.INVISIBLE
                    imageView.visibility = View.VISIBLE
                    imageView.setImageURI(resourceURI)
                    mediaType = "image"
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onUpload() {
        val userInfo = AppManager.getInstance(mainActivity).userInfo

        if (!validateFields()) return
        if (userInfo == null) return

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

        APIManager.getInstance(mainActivity).uploadFile(mapOf(
                "identifier" to identifier,
                "title" to title,
                "description" to description,
                "tags" to tags,
                "path" to resourcePath!!,
                "filename" to filename,
                "s3accesskey" to s3accesskey,
                "s3secretkey" to s3secretkey,
                "mediatype" to mediaType!!
        ), { success, uploaded, url, err ->
            mainActivity?.runOnUiThread({
                mainActivity?.hideProgressBar()

                val endTime = System.currentTimeMillis()
                val duration = formatTime((endTime - startTime) / 1000L)

                if (success) {
                    val txtView = TextView(context)
                    txtView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    val spannable = SpannableString("Uploaded $uploaded \n In $duration \n Available here " +
                            "https://archive.org/details/$identifier")
                    val iStart = spannable.toString().indexOf("https://")
                    val iEnd = spannable.toString().length
                    val clickableSpan = object: ClickableSpan() {
                        override fun onClick(view: View?) {
                            val intent = Intent(mainActivity, WebpageActivity::class.java)
                            intent.putExtra("URL", spannable.toString().substring(iStart))
                            startActivity(intent)
                        }

                        override fun updateDrawState(ds: TextPaint?) {
                            super.updateDrawState(ds)
                            ds?.isUnderlineText = false
                            ds?.color = resources.getColor(R.color.fcBlue)
                        }
                    }
                    spannable.setSpan(clickableSpan, iStart, iEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    txtView.text = spannable
                    txtView.movementMethod = LinkMovementMethod.getInstance()

                    SweetAlertDialog(mainActivity, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Successfully Uploaded")
                            .setCustomView(txtView)
                            .setConfirmText("OK")
                            .show()
                } else {
                    SweetAlertDialog(mainActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Uploading failed")
                            .setContentText(err)
                            .setConfirmText("OK")
                            .show()
                }
            })
        })
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
        val minutes_in_an_hour = 60
        val seconds_in_a_minute = 60

        val seconds = totalSeconds % seconds_in_a_minute
        val totalMinutes = totalSeconds / seconds_in_a_minute
        val minutes = totalMinutes % minutes_in_an_hour
        val hours = totalMinutes / minutes_in_an_hour

        var ret: String = ""
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
