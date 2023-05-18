package com.archive.waybackmachine.activity

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import com.archive.waybackmachine.R
import com.archive.waybackmachine.helper.AndroidLifecycleUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.chrisbanes.photoview.PhotoView
import java.io.File

class PhotoPreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_preview)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        val photos = intent.getStringArrayListExtra("photos")
        if (photos != null)
            viewPager.adapter = SamplePagerAdapter(this, photos.toList())
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    internal class SamplePagerAdapter(context: Context, val photos: List<String>) : PagerAdapter() {

        private var mContext: Context

        init {
            mContext = context
        }

        override fun getCount(): Int = photos.size

        override fun instantiateItem(container: ViewGroup, position: Int): View {
            val photoView = PhotoView(container.context)
            val imagePath = photos[position]
            val uri = Uri.fromFile(File(imagePath))

            val canLoadImage = AndroidLifecycleUtils.canLoadImage(photoView.context)

            if (canLoadImage) {
                val options = RequestOptions()
                options.centerCrop()
                        .placeholder(R.mipmap.image_placeholder)
                        .error(R.mipmap.broken_image_black_48dp)
                Glide.with(mContext)
                        .load(uri)
                        .apply(options)
                        .thumbnail(0.2f)
                        .into(photoView)
            }

            // Now just add PhotoView to ViewPager and return it
            container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            return photoView
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            container.removeView(obj as View)
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view === obj
        }

    }
}

