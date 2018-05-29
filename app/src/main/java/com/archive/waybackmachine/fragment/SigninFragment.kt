package com.archive.waybackmachine.fragment


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.archive.waybackmachine.R
import com.archive.waybackmachine.activity.MainActivity
import com.archive.waybackmachine.global.APIManager
import com.archive.waybackmachine.global.AppManager
import com.archive.waybackmachine.model.UserModel
import kotlinx.android.synthetic.main.fragment_signin.*
import kotlinx.android.synthetic.main.fragment_signin.view.*

class SigninFragment : Fragment(), View.OnClickListener {

    private var mainActivity: MainActivity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_signin, container, false)
        view.btnLogin.setOnClickListener(this)
        view.btnSignUp.setOnClickListener(this)
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
            R.id.btnLogin -> {
                if (txtEmail.text.isEmpty()) {
                    AppManager.getInstance(mainActivity).displayToast("Please enter your email")
                    return
                }

                if (txtPassword.text.isEmpty()) {
                    AppManager.getInstance(mainActivity).displayToast("Please enter your password")
                    return
                }

                login(txtEmail.text.toString(), txtPassword.text.toString())
            }
            R.id.btnSignUp -> {
                mainActivity?.replaceSignupFragment()
            }
        }
    }

    private fun login(email: String, password: String) {
        mainActivity?.showProgressBar()

        APIManager.getInstance(mainActivity).login(email, password, {success, error ->
            if (!success) {
                AppManager.getInstance(mainActivity).displayToast(error!!)
            } else {
                APIManager.getInstance(mainActivity).getUsername(email, {_, username, error ->
                    APIManager.getInstance(mainActivity).getCookieData(email, password, {_, loggedInSig, loggedInUser, _ ->
                        APIManager.getInstance(mainActivity).getIAS3Keys(loggedInSig!!, loggedInUser!!, {_, access, secret, _ ->

                            mainActivity?.runOnUiThread({
                                mainActivity?.hideProgressBar()
                                AppManager.getInstance(mainActivity).userInfo = UserModel(
                                        username!!, email, loggedInSig, loggedInUser, password, access!!, secret!!
                                )
                                mainActivity?.replaceAccountFragment()
                            })
                        })
                    })
                })
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                SigninFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
