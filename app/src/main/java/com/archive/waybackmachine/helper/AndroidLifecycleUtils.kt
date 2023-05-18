package com.archive.waybackmachine.helper

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import androidx.fragment.app.Fragment


class AndroidLifecycleUtils {

    companion object {

        fun canLoadImage(fragment: Fragment?): Boolean {
            if (fragment == null) {
                return true
            }

            return canLoadImage(fragment.activity)
        }

        fun canLoadImage(context: Context?): Boolean {
            if (context == null) {
                return true
            }

            if (context !is AppCompatActivity) {
                return true
            }

            return canLoadImage(context as AppCompatActivity?)
        }

        fun canLoadImage(activity: AppCompatActivity?): Boolean {
            if (activity == null) {
                return true
            }

            val destroyed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed

            return !(destroyed || activity.isFinishing)
        }
    }

}
