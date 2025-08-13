package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.MainActivity
import com.internetarchive.waybackmachine.global.AppManager

class AccountFragment : Fragment(), View.OnClickListener {

    private var mainActivity: MainActivity? = null
    private lateinit var btnLogout: Button
    private lateinit var txtDescription: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        
        // Initialize views
        btnLogout = view.findViewById(R.id.btnLogout)
        txtDescription = view.findViewById(R.id.txtDescription)
        
        btnLogout.setOnClickListener(this)

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            if (context is MainActivity) {
                mainActivity = context
                android.util.Log.d("AccountFragment", "Successfully attached to MainActivity")
            } else {
                android.util.Log.w("AccountFragment", "Context is not MainActivity: ${context.javaClass.simpleName}")
            }
        } catch (e: Exception) {
            android.util.Log.e("AccountFragment", "Error in onAttach", e)
        }
    }

    override fun onResume() {
        super.onResume()

        val userInfo = AppManager.getInstance(mainActivity).userInfo

        if (userInfo != null) {
            val username = userInfo.username
            val description = resources.getString(R.string.logged_in_description, username)
            txtDescription.text = description
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
