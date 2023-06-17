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

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.MainActivity
import com.internetarchive.waybackmachine.activity.WebpageActivity
import com.internetarchive.waybackmachine.global.APIManager
import com.internetarchive.waybackmachine.global.AppManager
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : Fragment(), View.OnClickListener {

    private var mainActivity: MainActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        view.btnSave.setOnClickListener(this)
        view.btnRecent.setOnClickListener(this)
        view.btnFirst.setOnClickListener(this)
        view.btnOverview.setOnClickListener(this)

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
        var ret = url
        val tmpArray = url.split("http")

        when (tmpArray.count()) {
            1 -> {
                ret = "http://$ret"
            }
            3 -> {
                ret = "http://$tmpArray[2]"
            }
        }

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
