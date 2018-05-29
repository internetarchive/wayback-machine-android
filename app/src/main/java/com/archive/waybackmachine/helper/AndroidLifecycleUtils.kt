package com.archive.waybackmachine.helper

import android.os.Build
import android.app.Activity
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity


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

            if (context !is Activity) {
                return true
            }

            return canLoadImage(context as Activity?)
        }

        fun canLoadImage(activity: Activity?): Boolean {
            if (activity == null) {
                return true
            }

            val destroyed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed

            return !(destroyed || activity.isFinishing)
        }
    }

}
