package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.MainActivity
import com.internetarchive.waybackmachine.global.APIManager
import com.internetarchive.waybackmachine.global.AppManager
import com.internetarchive.waybackmachine.model.UserModel
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

        APIManager.getInstance(mainActivity).login(email, password) { success, error, data->
            if (!success) {
                AppManager.getInstance(mainActivity).displayToast(error!!)
                mainActivity?.hideProgressBar()
            } else {

                // Retrieve the "values" JSONObject
                val valuesObject = data?.getJSONObject("values")

                // Retrieve the "cookies" JSONObject
                val cookiesObject = valuesObject?.getJSONObject("cookies")

                // Retrive the s3 JSONObject
                val s3Data = valuesObject?.getJSONObject("s3")

                // Retrieve the value of the "logged-in-sig" key
                val loggedInSig = cookiesObject?.getString("logged-in-sig")
                val loggedInUser = valuesObject?.getString("email")
                val username = valuesObject?.getString("screenname")
                val access = s3Data?.getString("access")
                val secret = s3Data?.getString("secret")

                mainActivity?.runOnUiThread {
                    mainActivity?.hideProgressBar()
                    AppManager.getInstance(mainActivity).userInfo = UserModel(
                        username!!,
                        email,
                        loggedInSig,
                        loggedInUser,
                        password,
                        access!!,
                        secret!!
                    )
                    mainActivity?.replaceAccountFragment()
                }
            }
        }
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
