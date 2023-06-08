package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.MainActivity
import com.internetarchive.waybackmachine.global.AppManager
import kotlinx.android.synthetic.main.fragment_account.view.*

class AccountFragment : Fragment(), View.OnClickListener {

    private var mainActivity: MainActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        view.btnLogout.setOnClickListener(this)

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is MainActivity) {
            mainActivity = context
        }
    }

    override fun onResume() {
        super.onResume()

        var userInfo = AppManager.getInstance(mainActivity).userInfo

        if (userInfo != null) {
            view?.txtDescription?.text = "You're logged into the Internet Archive as ${userInfo.username}"
        }
    }

    override fun onClick(v: View?) {
        if (v == null) return

        when (v.id) {
            R.id.btnLogout -> {
                onLogout()
            }
        }
    }

    private fun onLogout() {
        AppManager.getInstance(mainActivity).userInfo = null
        mainActivity?.selectFragmentWithIndex(3)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                AccountFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
