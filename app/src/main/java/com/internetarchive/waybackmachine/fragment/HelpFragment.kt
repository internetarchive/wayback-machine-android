package com.internetarchive.waybackmachine.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.adapter.HelpPagerAdapter
import kotlinx.android.synthetic.main.fragment_help.view.*

class HelpFragment : Fragment() {
    private lateinit var mContext: Context

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_help, container, false)

        view.viewPager.adapter = HelpPagerAdapter()
        view.viewPager.currentItem = 0
        view.pageIndicatorView.setViewPager(view.viewPager)

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
