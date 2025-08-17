package com.internetarchive.waybackmachine.global

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.internetarchive.waybackmachine.model.UserModel

private const val PreferencesKey = "com.internetarchive.waybackmachine"
private const val UsernameKey = "com.internetarchive.waybackmachine.username"
private const val EmailKey = "com.internetarchive.waybackmachine.email"
private const val LoggedInSigKey = "com.internetarchive.waybackmachine.loggedInSig"
private const val LoggedInUserKey = "com.internetarchive.waybackmachine.loggedInUser"
private const val PasswordKey = "com.internetarchive.waybackmachine.password"
private const val S3AccessKey = "com.internetarchive.waybackmachine.s3AccessKey"
private const val S3SecretKey = "com.internetarchive.waybackmachine.s3SecretKey"

class AppManager private constructor(context: Context?) {
    companion object : SingletonHolder<AppManager, Context?>(::AppManager)

    private var mContext: Context? = null
    private var prefs: SharedPreferences? = null

    init {
        try {
            android.util.Log.d("AppManager", "🔧 Initializing AppManager")
            mContext = context
            prefs = mContext?.getSharedPreferences(PreferencesKey, Context.MODE_PRIVATE)
            
            // Check if stored user data is valid, if not clear it
            cleanupInvalidUserData()
            
            android.util.Log.d("AppManager", "✅ AppManager initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("AppManager", "❌ Error initializing AppManager", e)
        }
    }
    
    private fun cleanupInvalidUserData() {
        try {
            val currentUserInfo = userInfo
            if (currentUserInfo != null) {
                // If userInfo is not null but doesn't meet our validation criteria, clear it
                if (currentUserInfo.username.isEmpty() || currentUserInfo.email.isEmpty() || 
                    currentUserInfo.loggedInSig.isEmpty() || currentUserInfo.loggedInSig.length <= 50) {
                    android.util.Log.d("AppManager", "🧹 Cleaning up invalid user data")
                    userInfo = null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppManager", "❌ Error cleaning up invalid user data", e)
        }
    }

    val WebURL = "https://web.archive.org/"

    var userInfo: UserModel?
        set(userData) {
            val editor = prefs?.edit()
            if (editor != null) {
                if (userData != null) {
                    editor.putString(UsernameKey, userData.username ?: "")
                    editor.putString(EmailKey, userData.email ?: "")
                    editor.putString(LoggedInSigKey, userData.loggedInSig ?: "")
                    editor.putString(LoggedInUserKey, userData.loggedInUser ?: "")
                    editor.putString(PasswordKey, userData.password ?: "")
                    editor.putString(S3AccessKey, userData.s3AccessKey ?: "")
                    editor.putString(S3SecretKey, userData.s3SecretKey ?: "")
                    editor.apply()
                } else {
                    editor.putString(UsernameKey, "")
                    editor.putString(EmailKey, "")
                    editor.putString(LoggedInSigKey, "")
                    editor.putString(LoggedInUserKey, "")
                    editor.putString(PasswordKey, "")
                    editor.putString(S3AccessKey, "")
                    editor.putString(S3SecretKey, "")
                    editor.apply()
                }
            } else {
                android.util.Log.e("AppManager", "Failed to get SharedPreferences editor")
            }
        }
        get() {
            val preferences = this.prefs
            return if (preferences != null) {
                val userModel = createUserModelFromPreferences(preferences)
                userModel
            } else {
                android.util.Log.e("AppManager", "SharedPreferences is null")
                null
            }
        }

    private fun createUserModelFromPreferences(preferences: SharedPreferences): UserModel? {
        val usernameStr: String = preferences.getString(UsernameKey, "") ?: ""
        val emailStr: String = preferences.getString(EmailKey, "") ?: ""
        val loggedInSigStr: String = preferences.getString(LoggedInSigKey, "") ?: ""
        val loggedInUserStr: String = preferences.getString(LoggedInUserKey, "") ?: ""
        val passwordStr: String = preferences.getString(PasswordKey, "") ?: ""
        val s3AccessKeyStr: String = preferences.getString(S3AccessKey, "") ?: ""
        val s3SecretKeyStr: String = preferences.getString(S3SecretKey, "") ?: ""

        // More robust login status check: user is only considered logged in if they have valid credentials
        return if (usernameStr.isNotEmpty() && emailStr.isNotEmpty() && loggedInSigStr.isNotEmpty() && loggedInSigStr.length > 50) {
            UserModel(
                username = usernameStr,
                email = emailStr,
                loggedInSig = loggedInSigStr,
                loggedInUser = loggedInUserStr,
                password = passwordStr,
                s3AccessKey = s3AccessKeyStr,
                s3SecretKey = s3SecretKeyStr
            )
        } else {
            null
        }
    }

    fun getVersionName(): String {
        val context = this.mContext

        return if (context != null) {
            context.packageManager.getPackageInfo(context.packageName, 0)
                    .versionName
        } else {
            ""
        }
    }

    fun displayToast(message: String) {
        if (mContext == null) return

        val toast = Toast.makeText(mContext!!, message, Toast.LENGTH_SHORT)
        toast.show()
    }
}