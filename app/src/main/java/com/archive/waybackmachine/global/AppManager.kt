package com.archive.waybackmachine.global

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.archive.waybackmachine.model.UserModel

private const val PreferencesKey = "com.archive.waybackmachine"
private const val UsernameKey = "com.archive.waybackmachine.username"
private const val EmailKey = "com.archive.waybackmachine.email"
private const val LoggedInSigKey = "com.archive.waybackmachine.loggedInSig"
private const val LoggedInUserKey = "com.archive.waybackmachine.loggedInUser"
private const val PasswordKey = "com.archive.waybackmachine.password"
private const val S3AccessKey = "com.archive.waybackmachine.s3AccessKey"
private const val S3SecretKey = "com.archive.waybackmachine.s3SecretKey"

class AppManager private constructor(context: Context?) {
    companion object : SingletonHolder<AppManager, Context?>(::AppManager)

    private var mContext: Context? = null
    private var prefs: SharedPreferences? = null

    init {
        mContext = context
        prefs = mContext?.getSharedPreferences(PreferencesKey, 0)
    }

    val WebURL = "https://web.archive.org/"

    var userInfo: UserModel?
        set(userData) {
            val editor = prefs?.edit()
            if (editor != null) {
                if (userData != null) {
                    editor.putString(UsernameKey, userData.username)
                    editor.putString(EmailKey, userData.email)
                    editor.putString(LoggedInSigKey, userData.loggedInSig)
                    editor.putString(LoggedInUserKey, userData.loggedInUser)
                    editor.putString(PasswordKey, userData.password)
                    editor.putString(S3AccessKey, userData.s3AccessKey)
                    editor.putString(S3SecretKey, userData.s3SecretKey)
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
            }
        }
        get() {
            val preferences = this.prefs
            return if (preferences != null) {
                val username = preferences.getString(UsernameKey, "")
                val email = preferences.getString(EmailKey, "")
                val loggedInSig = preferences.getString(LoggedInSigKey, "")
                val loggedInUser = preferences.getString(LoggedInUserKey, "")
                val password = preferences.getString(PasswordKey, "")
                val s3AccessKey = preferences.getString(S3AccessKey, "")
                val s3SecretKey = preferences.getString(S3SecretKey, "")

                if (username.isNullOrEmpty() && email.isNullOrEmpty()) {
                    null
                } else {
                    UserModel(username!!, email!!, loggedInSig!!, loggedInUser!!, password!!, s3AccessKey!!, s3SecretKey!!)
                }
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