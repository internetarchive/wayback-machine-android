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
import kotlinx.android.synthetic.main.fragment_signup.*
import kotlinx.android.synthetic.main.fragment_signup.view.*


class SignupFragment : Fragment(), View.OnClickListener {

    private var mainActivity: MainActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_signup, container, false)
        // view.btnContinue.setOnClickListener(this)
        // view.btnCancel.setOnClickListener(this)

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

    // private fun onContinue() {
    //     if (!validateFields()) return

    //     mainActivity?.showProgressBar()

    //     val username = ctxtUsername.text.toString()
    //     val email = ctxtEmail.text.toString()
    //     val password = ctxtPassword.text.toString()

    //     APIManager.getInstance(mainActivity).registerAccount(email, password, username) { success, err ->
    //         mainActivity?.runOnUiThread {
    //             mainActivity?.hideProgressBar()
    //             if (success) {
    //                 AppManager.getInstance(mainActivity)
    //                     .displayToast("We just sent verification email. Please try to verify your account.")
    //                 mainActivity?.replaceSigninFragment()
    //             } else {
    //                 AppManager.getInstance(mainActivity).displayToast(err!!)
    //             }
    //         }
    //     }
    // }

    // private fun validateFields(): Boolean {
    //     if (ctxtUsername.text.isEmpty()) {
    //         AppManager.getInstance(mainActivity).displayToast("Please enter your username")
    //         return false
    //     }
    //     if (ctxtEmail.text.isEmpty()) {
    //         AppManager.getInstance(mainActivity).displayToast("Please enter your email")
    //         return false
    //     }
    //     if (ctxtPassword.text.isEmpty()) {
    //         AppManager.getInstance(mainActivity).displayToast("Please enter your password")
    //         return false
    //     }
    //     if (ctxtConfirmPassword.text.isEmpty()) {
    //         AppManager.getInstance(mainActivity).displayToast("Please confirm your password")
    //         return false
    //     }
    //     if (ctxtPassword.text.toString() != ctxtConfirmPassword.text.toString()) {
    //         AppManager.getInstance(mainActivity).displayToast("Password does not match")
    //         ctxtPassword.text.clear()
    //         ctxtConfirmPassword.text.clear()
    //         return false
    //     }

    //     return true
    // }

    companion object {
        @JvmStatic
        fun newInstance() =
                SignupFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
