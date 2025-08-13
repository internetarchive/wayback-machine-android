package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.MainActivity
import com.internetarchive.waybackmachine.global.AppManager
import com.internetarchive.waybackmachine.global.APIManager

class SignupFragment : Fragment(), View.OnClickListener {

    private var mainActivity: MainActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_signup, container, false)
        // Note: Most functionality is commented out in the original code
        
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is MainActivity) {
            mainActivity = context
        }
    }

    override fun onClick(v: View?) {
        if (v == null) return

        when (v.id) {
            R.id.btnCancel -> {
                mainActivity?.replaceSigninFragment()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                SignupFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
