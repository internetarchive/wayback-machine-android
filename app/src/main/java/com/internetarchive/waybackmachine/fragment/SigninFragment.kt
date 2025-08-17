package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.MainActivity
import com.internetarchive.waybackmachine.activity.WebpageActivity
import com.internetarchive.waybackmachine.global.APIManager
import com.internetarchive.waybackmachine.global.AppManager
import com.internetarchive.waybackmachine.model.UserModel

class SigninFragment : Fragment(), View.OnClickListener {

    private var mainActivity: MainActivity? = null
    
    // View references
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: TextView
    private lateinit var txtEmail: EditText
    private lateinit var txtPassword: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_signin, container, false)
        
        // Initialize views
        btnLogin = view.findViewById(R.id.btnLogin)
        btnSignUp = view.findViewById(R.id.btnSignUp) // Fixed: using correct ID
        txtEmail = view.findViewById(R.id.txtEmail)
        txtPassword = view.findViewById(R.id.txtPassword)
        
        // Make the signup TextView clickable and style it like a button
        btnSignUp.isClickable = true
        btnSignUp.isFocusable = true
        
        btnLogin.setOnClickListener(this)
        btnSignUp.setOnClickListener(this)
        
        // Check if user is already logged in and show appropriate UI
        checkAndUpdateUI()
        
        return view
    }
    
    private fun checkAndUpdateUI() {
        val userInfo = AppManager.getInstance(mainActivity).userInfo
        if (userInfo != null && userInfo.loggedInSig.isNotEmpty() && 
            userInfo.username.isNotEmpty() && userInfo.email.isNotEmpty() &&
            userInfo.loggedInSig.length > 50) {
            
            // User is logged in, show logout option
            android.util.Log.d("SigninFragment", "User is logged in, updating UI to show logout option")
            btnLogin.text = "Logout & Login Again"
            txtEmail.setText(userInfo.email)
            txtPassword.setText("") // Clear password for security
        } else {
            // User is not logged in, show normal login UI
            android.util.Log.d("SigninFragment", "User is not logged in, showing normal login UI")
            btnLogin.text = "Login"
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            if (context is MainActivity) {
                mainActivity = context
                android.util.Log.d("SigninFragment", "Successfully attached to MainActivity")
                
                // Don't auto-redirect here - let MainActivity handle navigation
                // Just log the current state for debugging
                val userInfo = AppManager.getInstance(mainActivity).userInfo
                android.util.Log.d("SigninFragment", "onAttach - Current userInfo: $userInfo")
                
                if (userInfo != null) {
                    android.util.Log.d("SigninFragment", "UserInfo details - loggedInSig: '${userInfo.loggedInSig}', username: '${userInfo.username}', email: '${userInfo.email}'")
                }
            } else {
                android.util.Log.w("SigninFragment", "Context is not MainActivity: ${context.javaClass.simpleName}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SigninFragment", "Error in onAttach", e)
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Don't auto-redirect here - let MainActivity handle navigation
        // Just update the UI based on current login status
        try {
            android.util.Log.d("SigninFragment", "onResume - updating UI based on current login status")
            checkAndUpdateUI()
        } catch (e: Exception) {
            android.util.Log.e("SigninFragment", "Error updating UI in onResume", e)
        }
    }

    override fun onClick(v: View?) {
        if (v == null) return

        when (v.id) {
            R.id.btnLogin -> {
                val userInfo = AppManager.getInstance(mainActivity).userInfo
                val isCurrentlyLoggedIn = userInfo != null && userInfo.loggedInSig.isNotEmpty() && 
                                        userInfo.username.isNotEmpty() && userInfo.email.isNotEmpty() &&
                                        userInfo.loggedInSig.length > 50
                
                if (isCurrentlyLoggedIn) {
                    // User is currently logged in, handle logout
                    android.util.Log.d("SigninFragment", "User clicked logout, clearing session")
                    AppManager.getInstance(mainActivity).userInfo = null
                    checkAndUpdateUI()
                    AppManager.getInstance(mainActivity).displayToast("Logged out successfully. Please login again.")
                } else {
                    // User is not logged in, handle login
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
            }
            R.id.btnSignUp -> {
                openWebPage("https://archive.org/account/signup")
            }
        }
    }

    private fun openWebPage(url: String) {
        val intent = Intent(context, WebpageActivity::class.java)
        intent.putExtra("URL", url)
        startActivity(intent)
    }

    private fun login(email: String, password: String) {
        mainActivity?.showProgressBar()
        android.util.Log.d("SigninFragment", "Starting login process for email: $email")

        APIManager.getInstance(mainActivity).login(email, password) { success, error, data->
            android.util.Log.d("SigninFragment", "Login callback received - success: $success, error: $error")
            
            if (!success) {
                android.util.Log.e("SigninFragment", "Login failed: $error")
                AppManager.getInstance(mainActivity).displayToast(error!!)
                mainActivity?.hideProgressBar()
            } else {
                android.util.Log.d("SigninFragment", "Login successful, processing response data")
                android.util.Log.d("SigninFragment", "Raw response data: $data")

                // Retrieve the "values" JSONObject
                val valuesObject = data?.getJSONObject("values")
                android.util.Log.d("SigninFragment", "Values object: $valuesObject")

                // Retrieve the "cookies" JSONObject
                val cookiesObject = valuesObject?.getJSONObject("cookies")
                android.util.Log.d("SigninFragment", "Cookies object: $cookiesObject")

                // Retrieve the s3 JSONObject
                val s3Data = valuesObject?.getJSONObject("s3")
                android.util.Log.d("SigninFragment", "S3 data: $s3Data")

                // Retrieve the value of the "logged-in-sig" key
                val loggedInSig = cookiesObject?.getString("logged-in-sig")
                val loggedInUser = valuesObject?.getString("email")
                val username = valuesObject?.getString("screenname")
                val access = s3Data?.getString("access")
                val secret = s3Data?.getString("secret")

                android.util.Log.d("SigninFragment", "Extracted data - loggedInSig: $loggedInSig, loggedInUser: $loggedInUser, username: $username")

                mainActivity?.runOnUiThread {
                    android.util.Log.d("SigninFragment", "Running on UI thread to update user session")
                    mainActivity?.hideProgressBar()
                    
                    val userModel = UserModel(
                        username ?: "",
                        email,
                        loggedInSig ?: "",
                        loggedInUser ?: "",
                        password,
                        access ?: "",
                        secret ?: ""
                    )
                    
                    AppManager.getInstance(mainActivity).userInfo = userModel
                    android.util.Log.d("SigninFragment", "User session stored successfully: $userModel")
                    
                    android.util.Log.d("SigninFragment", "Login successful! Forcing navigation to account section")
                    
                    // Force navigation to account section to show the logged-in state
                    mainActivity?.runOnUiThread {
                        // Update navigation state and replace with account fragment
                        mainActivity?.updateNavigationToAccount()
                        mainActivity?.replaceAccountFragment()
                    }
                    
                    android.util.Log.d("SigninFragment", "Navigation to account section initiated")
                }
            }
        }
    }

    private fun logout() {
        try {
            android.util.Log.d("SigninFragment", "Logging out user and clearing session")
            
            // Clear the user session
            AppManager.getInstance(mainActivity).userInfo = null
            
            // Update the UI to show login form
            checkAndUpdateUI()
            
            // Show logout message
            AppManager.getInstance(mainActivity).displayToast("Logged out successfully")
            
            android.util.Log.d("SigninFragment", "Logout completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("SigninFragment", "Error during logout", e)
            AppManager.getInstance(mainActivity).displayToast("Logout failed: ${e.message}")
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
