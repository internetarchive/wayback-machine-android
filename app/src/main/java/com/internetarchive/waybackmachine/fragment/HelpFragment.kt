package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.adapter.HelpPagerAdapter

class HelpFragment : Fragment() {
    private lateinit var mContext: Context

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_help, container, false)

        try {
            // Initialize views
            val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
            val tabLayout = view.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)

            // Create adapter
            val adapter = HelpPagerAdapter()
            viewPager.adapter = adapter

            // Connect ViewPager2 with TabLayout using the modern approach
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                when (position) {
                    0 -> tab.text = "Welcome"
                    1 -> tab.text = "How to Use"
                    2 -> tab.text = "About"
                }
            }.attach()

            android.util.Log.d("HelpFragment", "Help section initialized successfully with ${adapter.itemCount} pages")
            
        } catch (e: Exception) {
            android.util.Log.e("HelpFragment", "Failed to initialize help section", e)
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                HelpFragment().apply {
                    arguments = Bundle().apply {
                    }
                }
    }
}
