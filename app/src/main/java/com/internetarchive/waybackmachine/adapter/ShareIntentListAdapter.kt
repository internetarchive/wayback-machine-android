package com.internetarchive.waybackmachine.adapter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.internetarchive.waybackmachine.R
import androidx.core.content.ContextCompat


class ShareIntentListAdapter(private val context: Context, private val shareLink: String?) : BaseAdapter() {
    private val listResolve: MutableList<ResolveInfo>?
    private val pm: PackageManager = context.packageManager
    private var typeface: Typeface? = null
    private var currentPosition = -1

    init {
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareLink)
        listResolve = pm.queryIntentActivities(sendIntent, 0)

        for (i in listResolve.indices) {
            if (listResolve[i].loadLabel(pm).toString() == context.resources.getString(R.string.app_name)) {
                listResolve.removeAt(i)
            }
        }

    }

//    fun setTypeface(tf: Typeface) {
//        typeface = tf
//    }

    override fun getCount(): Int {
        return listResolve?.size ?: 0
    }

    override fun getItem(position: Int): Any {
        return listResolve!![position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setCurrentPosition(position: Int) {
        currentPosition = position
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder?

        if (view == null) {
            val inflater = context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            holder = ViewHolder()
            view = inflater.inflate(R.layout.item_share, parent,
                    false)
            holder.image = view
                    .findViewById<View>(R.id.shareIntentItemIcon) as ImageView
            holder.text = view
                    .findViewById<View>(R.id.shareIntentItemText) as TextView
            if (typeface != null) {
                holder.text!!.typeface = typeface
            }

            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        val info = listResolve!![position]
        val icon = info.loadIcon(pm)
        holder.image!!.setImageDrawable(icon)
        holder.text!!.text = info.loadLabel(pm)
//        val activityInfo = info.activityInfo
//        val componentName = ComponentName(
//                activityInfo.applicationInfo.packageName, activityInfo.name)

        if (position == currentPosition) {
            view!!.setBackgroundColor(ContextCompat.getColor(context, R.color.shareContentColorYellow))
        } else {
            view!!.setBackgroundColor(ContextCompat.getColor(context, R.color.shareContentColorGreen))
        }

        return view
    }

    fun toggleSend(position: Int) {
        val info = listResolve!![position]
        val activityInfo = info.activityInfo
        val componentName = ComponentName(
                activityInfo.applicationInfo.packageName, activityInfo.name)

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.setClassName(activityInfo.packageName, activityInfo.name)
        intent.putExtra(Intent.EXTRA_TEXT, shareLink)
        intent.component = componentName
        context.startActivity(intent)
    }

    private inner class ViewHolder {
        var image: ImageView? = null
        var text: TextView? = null
    }

}