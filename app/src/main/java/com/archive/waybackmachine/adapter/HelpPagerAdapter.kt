package com.archive.waybackmachine.adapter

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import com.archive.waybackmachine.R

class HelpPagerAdapter : PagerAdapter() {

    override fun getCount(): Int {
        return 3
    }

    override fun instantiateItem(collection: View, position: Int): Any {
        val inflater = collection.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var resId = 0
        when (position) {
            0 -> resId = R.layout.page_help_1
            1 -> resId = R.layout.page_help_2
            2 -> resId = R.layout.page_help_3
        }

        val view = inflater!!.inflate(resId, null)
        (collection as ViewPager).addView(view, 0)

        return view
    }

    override fun destroyItem(container: View, position: Int, `object`: Any) {
        (container as ViewPager).removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as View
    }
}