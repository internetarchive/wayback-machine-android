package com.archive.waybackmachine.global


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

import java.util.ArrayList

class PermissionManager private constructor(context: Context?) {
    companion object : SingletonHolder<PermissionManager, Context?>(::PermissionManager)

    private var mContext: Context? = null
    val READ_EXTERNAL_STORAGE = 0

    init {
        mContext = context
    }

    fun requestPermissions(activity: Activity, permissions: IntArray, requestCode: Int) {

        val permissionArrayList = ArrayList<String>()

        for (permission in permissions) {
            if (permission == READ_EXTERNAL_STORAGE) {
                val permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    permissionArrayList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        val permissionStringAry = arrayOfNulls<String>(permissionArrayList.size)

        for (i in permissionArrayList.indices) {
            permissionStringAry[i] = permissionArrayList.toTypedArray()[i]
        }

        if (permissionStringAry.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionStringAry, requestCode)
        }

    }


}