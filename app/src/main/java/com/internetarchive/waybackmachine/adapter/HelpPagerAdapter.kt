package com.internetarchive.waybackmachine.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.internetarchive.waybackmachine.R

class HelpPagerAdapter : RecyclerView.Adapter<HelpPagerAdapter.HelpViewHolder>() {

    inner class HelpViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.page_help_1, parent, false)
        return HelpViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        // For now, all positions show the same content
        // You can customize this later to show different content for each position
        val inflater = LayoutInflater.from(holder.itemView.context)
        var resId = R.layout.page_help_1
        
        when (position) {
            0 -> resId = R.layout.page_help_1
            1 -> resId = R.layout.page_help_2
            2 -> resId = R.layout.page_help_3
        }
        
        // Clear the current view and inflate the new one
        (holder.itemView as ViewGroup).removeAllViews()
        val newView = inflater.inflate(resId, holder.itemView as ViewGroup, false)
        (holder.itemView as ViewGroup).addView(newView)
    }

    override fun getItemCount(): Int {
        return 3
    }
}
