package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.MainActivity
import com.internetarchive.waybackmachine.activity.WebpageActivity
import com.internetarchive.waybackmachine.global.APIManager
import com.internetarchive.waybackmachine.global.AppManager

class HomeFragment : Fragment(), View.OnClickListener {

    private var mainActivity: MainActivity? = null
    
    // View references
    private lateinit var btnSave: Button
    private lateinit var btnRecent: Button
    private lateinit var btnFirst: Button
    private lateinit var btnOverview: Button
    private lateinit var txtURL: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize views
        btnSave = view.findViewById(R.id.btnSave)
        btnRecent = view.findViewById(R.id.btnRecent)
        btnFirst = view.findViewById(R.id.btnFirst)
        btnOverview = view.findViewById(R.id.btnOverview)
        txtURL = view.findViewById(R.id.txtURL)

        btnSave.setOnClickListener(this)
        btnRecent.setOnClickListener(this)
        btnFirst.setOnClickListener(this)
        btnOverview.setOnClickListener(this)

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is MainActivity) {
            mainActivity = context
        }
    }

    override fun onPause() {
        if (txtURL.hasFocus()) {
            val imm = mainActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(txtURL.windowToken, 0)
        }

        super.onPause()
    }

    override fun onClick(v: View?) {
        if (v == null) return

        when (v.id) {
            R.id.btnSave -> {
                onSave()
            }
            R.id.btnRecent -> {
                onRecent()
            }
            R.id.btnFirst -> {
                onFirst()
            }
            R.id.btnOverview -> {
                onOverview()
            }
        }
    }

    private fun onSave() {
        val url = getURL(txtURL.text.toString())

        if (!validateURL(url)) {
            AppManager.getInstance(context).displayToast("Invalid URL")
        } else {
            openWebPage(AppManager.getInstance(context).WebURL + "save/" + url)
        }
    }

    private fun onRecent() {
        val url = getURL(txtURL.text.toString())

        if (!validateURL(url)) {
            AppManager.getInstance(context).displayToast("Invalid URL")
            return
        }

        mainActivity?.showProgressBar()

        APIManager.getInstance(context).checkPlaybackAvailability(url, "") { success, waybackURL ->
            mainActivity?.runOnUiThread {
                mainActivity?.hideProgressBar()
                if (!success) {
                    AppManager.getInstance(context)
                        .displayToast("Cannot connect to server. Please try again.")
                } else {
                    openWebPage(waybackURL!!)
                }
            }
        }

    }

    private fun onFirst() {
        val url = getURL(txtURL.text.toString())

        if (!validateURL(url)) {
            AppManager.getInstance(context).displayToast("Invalid URL")
            return
        }

        mainActivity?.showProgressBar()

        APIManager.getInstance(context).checkPlaybackAvailability(url, "00000000000000") { success, waybackURL ->
            mainActivity?.runOnUiThread {
                mainActivity?.hideProgressBar()
                if (!success) {
                    AppManager.getInstance(context)
                        .displayToast("Cannot connect to server. Please try again.")
                } else {
                    openWebPage(waybackURL!!)
                }
            }
        }
    }

    private fun onOverview() {
        val url = getURL(txtURL.text.toString())

        if (!validateURL(url)) {
            AppManager.getInstance(context).displayToast("Invalid URL")
            return
        }

        mainActivity?.showProgressBar()

        APIManager.getInstance(context).checkPlaybackAvailability(url, "00000000000000") { success, waybackURL ->
            mainActivity?.runOnUiThread {
                mainActivity?.hideProgressBar()
                if (!success) {
                    AppManager.getInstance(context)
                        .displayToast("Cannot connect to server. Please try again.")
                } else {
                    openWebPage(waybackURL!!)
                }
            }
        }
    }

    private fun openWebPage(url: String) {
        val intent = Intent(context, WebpageActivity::class.java)
        intent.putExtra("URL", url)
        startActivity(intent)
    }

    private fun validateURL(url: String) : Boolean {
        return Patterns.WEB_URL.matcher(url).matches()
    }

    private fun getURL(url: String): String {
        var ret = url.trim()
        
        // If URL doesn't start with http:// or https://, add http://
        if (!ret.startsWith("http://") && !ret.startsWith("https://")) {
            ret = "http://$ret"
        }
        
        // Remove any trailing slashes for consistency
        ret = ret.trimEnd('/')
        
        return ret
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            HomeFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}
