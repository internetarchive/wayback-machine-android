package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.content.Intent
import android.widget.ImageView
import android.widget.LinearLayout

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.activity.MainActivity
import com.internetarchive.waybackmachine.activity.WebpageActivity
import com.internetarchive.waybackmachine.global.AppManager
import com.internetarchive.waybackmachine.global.APIManager
import com.internetarchive.waybackmachine.model.UserModel

class SimpleProfileFragment : Fragment(), View.OnClickListener {

    private var mainActivity: MainActivity? = null
    
    // View references
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: TextView
    private lateinit var txtEmail: EditText
    private lateinit var txtPassword: EditText

    companion object {
        fun newInstance(): SimpleProfileFragment {
            return SimpleProfileFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            android.util.Log.d("SimpleProfileFragment", "Creating simple profile fragment view")
            
            // Create a proper profile layout instead of reusing login form
            val view = createProfileView()
            
            android.util.Log.d("SimpleProfileFragment", "Simple profile fragment view created successfully")
            return view
            
        } catch (e: Exception) {
            android.util.Log.e("SimpleProfileFragment", "Error in onCreateView", e)
            // Return a simple text view as fallback
            val textView = TextView(requireContext())
            textView.text = "Profile section loading..."
            textView.gravity = android.view.Gravity.CENTER
            return textView
        }
    }
    
    private fun createProfileView(): View {
        // Create a LinearLayout for the profile view
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(40, 40, 40, 40)
        }
        
        // Add the logo
        val logoImageView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 50, 0, 30)
            }
        }
        layout.addView(logoImageView)
        
        // Add welcome text
        val userInfo = AppManager.getInstance(mainActivity).userInfo
        val welcomeText = TextView(requireContext()).apply {
            text = "Welcome back!"
            textSize = 24f
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(R.color.fcBlack, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 10)
            }
        }
        layout.addView(welcomeText)
        
        // Add username
        val usernameText = TextView(requireContext()).apply {
            text = "User ID: ${userInfo?.username ?: "User"}"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(R.color.fcBlue, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 10, 0, 20)
            }
        }
        layout.addView(usernameText)
        
        // Add email with association message
        val emailText = TextView(requireContext()).apply {
            text = "Associated Email: ${userInfo?.email ?: ""}"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(resources.getColor(R.color.fcBlack, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 10, 0, 40)
            }
        }
        layout.addView(emailText)
        
        // Add logout button
        val logoutButton = Button(requireContext()).apply {
            text = "Logout"
            setBackgroundResource(R.drawable.button_background)
            setTextColor(resources.getColor(R.color.fcWhite, null))
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 20)
            }
            setOnClickListener { logout() }
        }
        layout.addView(logoutButton)
        
        return layout
    }
    
    override fun onAttach(context: Context) {
        try {
            super.onAttach(context)
            if (context is MainActivity) {
                mainActivity = context
                android.util.Log.d("SimpleProfileFragment", "Fragment attached successfully to MainActivity")
            } else {
                android.util.Log.w("SimpleProfileFragment", "Context is not MainActivity: ${context.javaClass.simpleName}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SimpleProfileFragment", "Error in onAttach", e)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            android.util.Log.d("SimpleProfileFragment", "Fragment created successfully")
        } catch (e: Exception) {
            android.util.Log.e("SimpleProfileFragment", "Error in onCreate", e)
        }
    }
    
    override fun onClick(v: View?) {
        if (v == null) return

        try {
            when (v.id) {
                R.id.btnLogin -> {
                    val userInfo = AppManager.getInstance(mainActivity).userInfo
                    val isCurrentlyLoggedIn = userInfo != null && userInfo.loggedInSig.isNotEmpty() && 
                                            userInfo.username.isNotEmpty() && userInfo.email.isNotEmpty() &&
                                            userInfo.loggedInSig.length > 50
                    
                    if (isCurrentlyLoggedIn) {
                        // User is logged in, handle logout
                        android.util.Log.d("SimpleProfileFragment", "User clicked logout")
                        logout()
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

                        // Perform actual login
                        login(txtEmail.text.toString(), txtPassword.text.toString())
                    }
                }
                R.id.btnSignUp -> {
                    // Only allow signup if user is not logged in
                    val userInfo = AppManager.getInstance(mainActivity).userInfo
                    if (userInfo == null || userInfo.loggedInSig.isEmpty()) {
                        openWebPage("https://archive.org/account/signup")
                    } else {
                        // User is logged in, show profile info
                        AppManager.getInstance(mainActivity).displayToast("Profile: ${userInfo.username} (${userInfo.email})")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SimpleProfileFragment", "Error in onClick", e)
            try {
                AppManager.getInstance(mainActivity).displayToast("Action failed: ${e.message}")
            } catch (e2: Exception) {
                android.util.Log.e("SimpleProfileFragment", "Failed to show error toast", e2)
            }
        }
    }
    
    private fun openWebPage(url: String) {
        try {
            val intent = Intent(context, WebpageActivity::class.java)
            intent.putExtra("URL", url)
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("SimpleProfileFragment", "Error opening webpage", e)
            try {
                AppManager.getInstance(mainActivity).displayToast("Failed to open signup page")
            } catch (e2: Exception) {
                android.util.Log.e("SimpleProfileFragment", "Failed to show error toast", e2)
            }
        }
    }
    
    private fun login(email: String, password: String) {
        try {
            mainActivity?.showProgressBar()
            
            APIManager.getInstance(mainActivity).login(email, password) { success, error, data ->
                mainActivity?.runOnUiThread {
                    mainActivity?.hideProgressBar()
                    
                    if (!success) {
                        AppManager.getInstance(mainActivity).displayToast(error ?: "Login failed")
                    } else {
                        try {
                            // Retrieve the "values" JSONObject
                            val valuesObject = data?.getJSONObject("values")
                            
                            // Retrieve the "cookies" JSONObject
                            val cookiesObject = valuesObject?.getJSONObject("cookies")
                            
                            // Retrieve the s3 JSONObject
                            val s3Data = valuesObject?.getJSONObject("s3")
                            
                            // Retrieve the value of the "logged-in-sig" key
                            val loggedInSig = cookiesObject?.getString("logged-in-sig")
                            val loggedInUser = valuesObject?.getString("email")
                            val username = valuesObject?.getString("screenname")
                            val access = s3Data?.getString("access")
                            val secret = s3Data?.getString("secret")
                            
                            AppManager.getInstance(mainActivity).userInfo = UserModel(
                                username ?: "",
                                email,
                                loggedInSig ?: "",
                                loggedInUser ?: "",
                                password,
                                access ?: "",
                                secret ?: ""
                            )
                            
                            // Show success message
                            AppManager.getInstance(mainActivity).displayToast("Login successful! Welcome, ${username ?: email}")
                            
                            // Navigate to account fragment or refresh the current view
                            // For now, just show success message
                            
                        } catch (e: Exception) {
                            android.util.Log.e("SimpleProfileFragment", "Error processing login response", e)
                            AppManager.getInstance(mainActivity).displayToast("Login successful but error processing user data")
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("SimpleProfileFragment", "Error in login", e)
            mainActivity?.hideProgressBar()
            try {
                AppManager.getInstance(mainActivity).displayToast("Login error: ${e.message}")
            } catch (e2: Exception) {
                android.util.Log.e("SimpleProfileFragment", "Failed to show error toast", e2)
            }
        }
    }

    private fun updateUIForLoggedInUser() {
        try {
            val userInfo = AppManager.getInstance(mainActivity).userInfo
            if (userInfo != null) {
                android.util.Log.d("SimpleProfileFragment", "Updating UI for logged-in user: ${userInfo.username}")
                
                // Update button text to show logout option
                btnLogin.text = "Logout"
                
                // Pre-fill email field with current user's email
                txtEmail.setText(userInfo.email)
                
                // Clear password field for security
                txtPassword.setText("")
                txtPassword.hint = "Password (hidden for security)"
                
                // Update signup text to show profile info
                btnSignUp.text = "Profile: ${userInfo.username}"
                btnSignUp.isClickable = false
                
            } else {
                android.util.Log.w("SimpleProfileFragment", "No user info found, showing default UI")
                btnLogin.text = "Login"
                btnSignUp.text = "Sign Up"
                btnSignUp.isClickable = true
            }
        } catch (e: Exception) {
            android.util.Log.e("SimpleProfileFragment", "Error updating UI for logged-in user", e)
        }
    }

    private fun logout() {
        try {
            // Clear the user session
            AppManager.getInstance(mainActivity).userInfo = null
            
            // Show logout message
            AppManager.getInstance(mainActivity).displayToast("Logged out successfully")
            
            // Force refresh of current fragment to ensure UI updates
            mainActivity?.runOnUiThread {
                // Navigate back to signin fragment
                mainActivity?.replaceSigninFragment()
            }
        } catch (e: Exception) {
            android.util.Log.e("SimpleProfileFragment", "Error during logout", e)
            AppManager.getInstance(mainActivity).displayToast("Logout failed: ${e.message}")
        }
    }
} 