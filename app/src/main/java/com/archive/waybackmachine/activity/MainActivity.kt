package com.archive.waybackmachine.activity

import android.os.Bundle
import android.os.PersistableBundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.archive.waybackmachine.R
import com.archive.waybackmachine.fragment.*
import com.archive.waybackmachine.global.AppManager
import com.archive.waybackmachine.global.PermissionManager
import com.archive.waybackmachine.helper.BottomNavigationViewHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val SELECTED_MENU_ITEM = "arg_selected_tab"
    private var mSelectedMenuItem: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BottomNavigationViewHelper.disableShiftMode(navView)
        containerIndicator.visibility = View.INVISIBLE

        navView.setOnNavigationItemSelectedListener(this)

        var selectedMenuItem: MenuItem

        if (savedInstanceState != null) {
            mSelectedMenuItem = savedInstanceState.getInt(SELECTED_MENU_ITEM, 0)
            selectedMenuItem = navView.menu.findItem(mSelectedMenuItem)
        } else {
            selectedMenuItem = navView.menu.getItem(0)
        }

        selectFragment(selectedMenuItem)

        PermissionManager.getInstance(this).requestPermissions(this, intArrayOf(0), 1)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (containerIndicator.visibility != View.VISIBLE) {
            selectFragment(item)
        }

        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SELECTED_MENU_ITEM, mSelectedMenuItem)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        val homeItem = navView.menu.getItem(0)
        if (mSelectedMenuItem != homeItem.itemId) {
            selectFragment(homeItem)
        } else {
            super.onBackPressed()
        }
    }

    private fun selectFragment(item: MenuItem) {
        var frag: Fragment? = null

        // init corresponding fragment
        when (item.itemId) {
            R.id.menu_home -> {
                frag = HomeFragment.newInstance()
            }
            R.id.menu_help -> {
                frag = HelpFragment.newInstance()
            }
            R.id.menu_about -> {
                frag = AboutFramgment.newInstance()
            }
            R.id.menu_account -> {
                if (AppManager.getInstance(this).userInfo == null) {
                    frag = SigninFragment.newInstance()
                } else {
                    frag = AccountFragment.newInstance()
                }
            }
            R.id.menu_upload -> {
                frag = UploadFragment.newInstance()
            }
        }

        mSelectedMenuItem = item.itemId
        navView.menu.findItem(mSelectedMenuItem).isChecked = true

        if (frag != null) {
            val manager = supportFragmentManager.beginTransaction()
            manager.replace(container.id, frag)
            manager.commit()
        }
    }

    fun selectFragmentWithIndex(index: Int) {
        selectFragment(navView.menu.getItem(index))
    }

    fun selectMenuItem(index: Int) {
        navView.selectedItemId = navView.menu.getItem(index).itemId
        selectFragment(navView.menu.getItem(index))
    }

    fun replaceSignupFragment() {
        val frag = SignupFragment.newInstance()
        val manager = supportFragmentManager.beginTransaction()
        manager.replace(container.id, frag)
        manager.commit()
    }

    fun replaceAccountFragment() {
        val frag = AccountFragment.newInstance()
        val manager = supportFragmentManager.beginTransaction()
        manager.replace(container.id, frag)
        manager.commit()
    }

    fun replaceSigninFragment() {
        val frag = SigninFragment.newInstance()
        val manager = supportFragmentManager.beginTransaction()
        manager.replace(container.id, frag)
        manager.commit()
    }

    fun showProgressBar() {
        containerIndicator.visibility = View.VISIBLE
    }

    fun hideProgressBar() {
        containerIndicator.visibility = View.INVISIBLE
    }
}


