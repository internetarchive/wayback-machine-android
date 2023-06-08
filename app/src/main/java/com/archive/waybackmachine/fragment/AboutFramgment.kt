package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.internetarchive.waybackmachine.R
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.fragment_about.view.*

class AboutFramgment : Fragment() {
    private lateinit var mContext: Context

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        val spannable = SpannableString(resources.getString(R.string.support))
        val str = spannable.toString()
        val iStart = str.indexOf("info@archive.org")
        val iEnd = iStart + 16
        val ssText = SpannableString(spannable)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "message/rfc822"
                intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("info@archive.org"))

                try {
                    startActivity(Intent.createChooser(intent, "Send mail..."))
                } catch (e: android.content.ActivityNotFoundException) {
                    Toast.makeText(activity,
                            "There are no email clients installed",
                            Toast.LENGTH_SHORT)
                            .show()
                }

            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = ContextCompat.getColor(mContext, R.color.fcBlue)
            }
        }

        ssText.setSpan(clickableSpan, iStart, iEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        view.txtSupport.text = ssText
        view.txtSupport.movementMethod = LinkMovementMethod.getInstance()

        view.txtVersion.text = view.txtVersion.text.toString() +
                activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                AboutFramgment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
