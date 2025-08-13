package com.internetarchive.waybackmachine.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.internetarchive.waybackmachine.R
import com.internetarchive.waybackmachine.fragment.*
import com.internetarchive.waybackmachine.global.AppManager
import com.internetarchive.waybackmachine.helper.BottomNavigationViewHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.content.Context
import android.widget.EditText
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.internetarchive.waybackmachine.fragment.SimpleProfileFragment
import android.app.AlertDialog

class MainActivity : AppCompatActivity() {

    private val SELECTED_MENU_ITEM = "arg_selected_tab"
    private var mSelectedMenuItem: Int = 0
    
    // View references
    internal lateinit var navView: BottomNavigationView
    private lateinit var containerIndicator: FrameLayout
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up comprehensive logging system
        setupComprehensiveLogging()

        // Set up global exception handler to catch any crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("MainActivity", "🚨 CRASH DETECTED in thread ${thread.name}", throwable)
            logCrashDetails(throwable)
            try {
                AppManager.getInstance(this).displayToast("App error occurred, but it's stable now")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to show error toast", e)
            }
        }

        // Initialize views
        navView = findViewById(R.id.navView)
        containerIndicator = findViewById(R.id.containerIndicator)
        container = findViewById(R.id.container)

        BottomNavigationViewHelper.disableShiftMode(navView)
        containerIndicator.visibility = View.INVISIBLE

        // Use modern navigation listener
        navView.setOnItemSelectedListener { item ->
            if (containerIndicator.visibility != View.VISIBLE) {
                // Log navigation attempt
                logNavigationAttempt(item)
                selectFragment(item)
            }
            true
        }

        val selectedMenuItem: MenuItem

        if (savedInstanceState != null) {
            mSelectedMenuItem = savedInstanceState.getInt(SELECTED_MENU_ITEM, 0)
            selectedMenuItem = navView.menu.findItem(mSelectedMenuItem)
        } else {
            selectedMenuItem = navView.menu.getItem(0)
        }

        selectFragment(selectedMenuItem)

        // Display current app version in logs
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            android.util.Log.d("MainActivity", "App Version: $versionName")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to get version info", e)
        }

        // Handle back press with modern API
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeItem = navView.menu.getItem(0)
                if (mSelectedMenuItem != homeItem.itemId) {
                    selectFragment(homeItem)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        //PermissionManager.getInstance(this).requestPermissions(this, intArrayOf(0), 1)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SELECTED_MENU_ITEM, mSelectedMenuItem)
        super.onSaveInstanceState(outState)
    }

    private fun selectFragment(item: MenuItem) {
        var frag: Fragment? = null

        try {
            android.util.Log.d("MainActivity", "Selecting fragment for item: ${item.title}")
            
            // init corresponding fragment
            when (item.itemId) {
                R.id.menu_home -> {
                    android.util.Log.d("MainActivity", "Creating HomeFragment")
                    frag = HomeFragment.newInstance()
                }
                R.id.menu_help -> {
                    android.util.Log.d("MainActivity", "Creating HelpFragment")
                    frag = HelpFragment.newInstance()
                }
                R.id.menu_about -> {
                    android.util.Log.d("MainActivity", "Creating AboutFragment")
                    frag = AboutFramgment.newInstance()
                }
                R.id.menu_account -> {
                    android.util.Log.d("MainActivity", "Creating Account/Signin Fragment")
                    
                    // Check if user is logged in to determine which fragment to show
                    val userInfo = AppManager.getInstance(this).userInfo
                    android.util.Log.d("MainActivity", "Account menu clicked - userInfo: $userInfo")
                    
                    if (userInfo != null) {
                        // User is logged in with valid credentials - show account/profile fragment
                        android.util.Log.d("MainActivity", "✅ User logged in with valid credentials, showing SimpleProfileFragment")
                        try {
                            frag = SimpleProfileFragment.newInstance()
                            logFragmentOperation("Creation", frag, true)
                            android.util.Log.d("MainActivity", "SimpleProfileFragment created successfully")
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "SimpleProfileFragment creation failed", e)
                            logFragmentOperation("Creation", null, false)
                            // Fallback to signin fragment
                            try {
                                AppManager.getInstance(this).displayToast("Profile section unavailable, showing login")
                                frag = SigninFragment.newInstance()
                            } catch (e2: Exception) {
                                android.util.Log.e("MainActivity", "Even fallback failed", e2)
                                frag = null
                            }
                        }
                    } else {
                        // User not logged in or has invalid credentials - show signin fragment
                        android.util.Log.d("MainActivity", "ℹ️ User not logged in or invalid credentials, showing SigninFragment")
                        frag = SigninFragment.newInstance()
                    }
                }
                R.id.menu_upload -> {
                    // Check if user is logged in before allowing access to upload section
                    val userInfo = AppManager.getInstance(this).userInfo
                    
                    if (userInfo != null) {
                        // User is logged in - allow access to upload section
                        frag = UploadFragment.newInstance()
                    } else {
                        // User is not logged in - show login dialog and redirect to login
                        
                        // Show login required dialog
                        AlertDialog.Builder(this)
                            .setTitle("Login Required")
                            .setMessage("You need to login to access the upload section")
                            .setPositiveButton("Login") { _, _ ->
                                // Navigate to login section
                                mSelectedMenuItem = R.id.menu_account
                                val accountItem = navView.menu.findItem(R.id.menu_account)
                                if (accountItem != null) {
                                    navView.selectedItemId = accountItem.itemId
                                }
                                selectFragment(accountItem)
                            }
                            .setNegativeButton("Cancel") { _, _ ->
                                // Stay on current section
                                // Reset navigation selection to current item
                                val currentItem = navView.menu.findItem(mSelectedMenuItem)
                                if (currentItem != null) {
                                    navView.selectedItemId = currentItem.itemId
                                }
                            }
                            .setCancelable(false)
                            .show()
                        
                        // Don't create upload fragment - user needs to login first
                        frag = null
                        return@selectFragment
                    }
                }
            }

            if (frag != null) {
                android.util.Log.d("MainActivity", "Fragment created successfully: ${frag.javaClass.simpleName}")
                mSelectedMenuItem = item.itemId
                
                // SIMPLE, BULLETPROOF FRAGMENT TRANSACTION
                try {
                    android.util.Log.d("MainActivity", "Starting simple fragment transaction")
                    
                    // Use the most basic transaction method possible
                    val transaction = supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.container, frag)
                    
                    // Try commitAllowingStateLoss first (safest)
                    try {
                        transaction.commitAllowingStateLoss()
                        logFragmentOperation("Transaction Commit", frag, true)
                        android.util.Log.d("MainActivity", "Fragment transaction committed successfully with commitAllowingStateLoss")
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "commitAllowingStateLoss failed, trying regular commit", e)
                        logFragmentOperation("Transaction Commit Fallback", frag, false)
                        // Fallback to regular commit
                        try {
                            transaction.commit()
                            logFragmentOperation("Transaction Commit Regular", frag, true)
                            android.util.Log.d("MainActivity", "Fragment transaction committed successfully with regular commit")
                        } catch (e2: Exception) {
                            android.util.Log.e("MainActivity", "Regular commit also failed", e2)
                            logFragmentOperation("Transaction Commit All Methods Failed", frag, false)
                            // Last resort: show error message
                            try {
                                AppManager.getInstance(this).displayToast("Navigation failed, but app is stable")
                            } catch (e3: Exception) {
                                android.util.Log.e("MainActivity", "Even toast failed", e3)
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Fragment transaction completely failed", e)
                    logFragmentOperation("Transaction Complete Failure", frag, false)
                    // Show error but don't crash
                    try {
                        AppManager.getInstance(this).displayToast("Navigation error: ${e.message}")
                    } catch (e2: Exception) {
                        android.util.Log.e("MainActivity", "Error display also failed", e2)
                    }
                }
            } else {
                android.util.Log.e("MainActivity", "Failed to create fragment for item: ${item.title}")
                logFragmentOperation("Fragment Creation Failed", null, false)
                // Show error message
                try {
                    AppManager.getInstance(this).displayToast("Failed to open ${item.title}")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error display failed", e)
                }
            }
        } catch (e: Exception) {
            // Log the error and show a toast
            android.util.Log.e("MainActivity", "Failed to create fragment", e)
            try {
                AppManager.getInstance(this).displayToast("Failed to open ${item.title}: ${e.message}")
            } catch (e2: Exception) {
                android.util.Log.e("MainActivity", "Failed to show error toast", e2)
            }
        }
    }

    fun selectFragmentWithIndex(index: Int) {
        selectFragment(navView.menu.getItem(index))
    }

    fun selectMenuItem(index: Int) {
        navView.selectedItemId = navView.menu.getItem(index).itemId
        selectFragment(navView.menu.getItem(index))
    }

    fun replaceAccountFragment() {
        try {
            android.util.Log.d("MainActivity", "🔄 replaceAccountFragment() called - starting fragment replacement")
            
            // Update the navigation state first
            mSelectedMenuItem = R.id.menu_account
            val accountItem = navView.menu.findItem(R.id.menu_account)
            if (accountItem != null) {
                navView.selectedItemId = accountItem.itemId
                android.util.Log.d("MainActivity", "✅ Navigation state updated to account section")
            } else {
                android.util.Log.w("MainActivity", "⚠️ Could not find account menu item")
            }
            
            // Create and replace the fragment - use SimpleProfileFragment to match navigation logic
            android.util.Log.d("MainActivity", "🔄 Creating SimpleProfileFragment instance")
            val frag = SimpleProfileFragment.newInstance()
            android.util.Log.d("MainActivity", "✅ SimpleProfileFragment created successfully: ${frag.javaClass.simpleName}")
            
            // Get current fragment info for debugging
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
            android.util.Log.d("MainActivity", "📱 Current fragment in container: ${currentFragment?.javaClass?.simpleName ?: "null"}")
            
            android.util.Log.d("MainActivity", "🔄 Starting fragment transaction")
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.container, frag)
            
            android.util.Log.d("MainActivity", "🔄 Committing transaction with commitAllowingStateLoss")
            transaction.commitAllowingStateLoss()
            
            android.util.Log.d("MainActivity", "✅ SimpleProfileFragment replacement transaction committed successfully")
            
            // Verify the fragment was added
            val newFragment = supportFragmentManager.findFragmentById(R.id.container)
            android.util.Log.d("MainActivity", "📱 New fragment in container: ${newFragment?.javaClass?.simpleName ?: "null"}")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to replace SimpleProfileFragment", e)
            // Fallback: try to show error message
            try {
                AppManager.getInstance(this).displayToast("Navigation failed: ${e.message}")
            } catch (e2: Exception) {
                android.util.Log.e("MainActivity", "❌ Even error display failed", e2)
            }
        }
    }

    fun replaceSigninFragment() {
        try {
            android.util.Log.d("MainActivity", "🔄 replaceSigninFragment() called - starting fragment replacement")
            
            // Update the navigation state to show account section
            mSelectedMenuItem = R.id.menu_account
            val accountItem = navView.menu.findItem(R.id.menu_account)
            if (accountItem != null) {
                navView.selectedItemId = accountItem.itemId
                android.util.Log.d("MainActivity", "✅ Navigation state updated to account")
            }
            
            // Create and replace the fragment
            android.util.Log.d("MainActivity", "🔄 Creating SigninFragment instance")
            val frag = SigninFragment.newInstance()
            android.util.Log.d("MainActivity", "✅ SigninFragment created successfully: ${frag.javaClass.simpleName}")
            
            // Get current fragment info for debugging
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
            android.util.Log.d("MainActivity", "📱 Current fragment in container: ${currentFragment?.javaClass?.simpleName ?: "null"}")
            
            android.util.Log.d("MainActivity", "🔄 Starting fragment transaction")
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.container, frag)
            
            android.util.Log.d("MainActivity", "🔄 Committing transaction with commitAllowingStateLoss")
            transaction.commitAllowingStateLoss()
            
            android.util.Log.d("MainActivity", "✅ SigninFragment replacement transaction committed successfully")
            
            // Verify the fragment was added
            val newFragment = supportFragmentManager.findFragmentById(R.id.container)
            android.util.Log.d("MainActivity", "📱 New fragment in container: ${newFragment?.javaClass?.simpleName ?: "null"}")
            
            // Add a small delay and check again to see if the fragment was properly attached
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val delayedFragment = supportFragmentManager.findFragmentById(R.id.container)
                android.util.Log.d("MainActivity", "📱 Delayed check - fragment in container: ${delayedFragment?.javaClass?.simpleName ?: "null"}")
            }, 1000)
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to replace SigninFragment", e)
            // Fallback: try to show error message
            try {
                AppManager.getInstance(this).displayToast("Navigation failed: ${e.message}")
            } catch (e2: Exception) {
                android.util.Log.e("MainActivity", "❌ Even error display failed", e2)
            }
        }
    }

    fun showProgressBar() {
        containerIndicator.visibility = View.VISIBLE
    }

    fun hideProgressBar() {
        containerIndicator.visibility = View.INVISIBLE
    }
    
    // Removed old anonymous fragment methods - now using SimpleProfileFragment
    
    // Removed old anonymous fragment methods - now using SimpleProfileFragment
    
    // ===== COMPREHENSIVE LOGGING SYSTEM =====
    
    private fun setupComprehensiveLogging() {
        try {
            android.util.Log.d("MainActivity", "🔍 Setting up comprehensive logging system")
            
            // Log app startup details
            logAppStartupDetails()
            
            // Set up lifecycle logging
            setupLifecycleLogging()
            
            // Set up fragment transaction logging
            setupFragmentTransactionLogging()
            
            android.util.Log.d("MainActivity", "✅ Comprehensive logging system initialized successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to setup logging system", e)
        }
    }
    
    private fun logAppStartupDetails() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            android.util.Log.i("MainActivity", "📱 App Startup Details:")
            android.util.Log.i("MainActivity", "   App Version: ${packageInfo.versionName}")
            android.util.Log.i("MainActivity", "   Version Code: ${packageInfo.versionCode}")
            android.util.Log.i("MainActivity", "   Package: ${packageInfo.packageName}")
            android.util.Log.i("MainActivity", "   Android SDK: ${android.os.Build.VERSION.SDK_INT}")
            android.util.Log.i("MainActivity", "   Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            android.util.Log.i("MainActivity", "   OS Version: ${android.os.Build.VERSION.RELEASE}")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to log app startup details", e)
        }
    }
    
    private fun setupLifecycleLogging() {
        // Override lifecycle methods to add logging
        lifecycle.addObserver(object : androidx.lifecycle.LifecycleObserver {
            @androidx.lifecycle.OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
            fun onCreate() {
                android.util.Log.d("MainActivity", "🔄 Lifecycle: onCreate")
            }
            
            @androidx.lifecycle.OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
            fun onStart() {
                android.util.Log.d("MainActivity", "🔄 Lifecycle: onStart")
            }
            
            @androidx.lifecycle.OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
            fun onResume() {
                android.util.Log.d("MainActivity", "🔄 Lifecycle: onResume")
            }
            
            @androidx.lifecycle.OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                android.util.Log.d("MainActivity", "🔄 Lifecycle: onPause")
            }
            
            @androidx.lifecycle.OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_STOP)
            fun onStop() {
                android.util.Log.d("MainActivity", "🔄 Lifecycle: onStop")
            }
            
            @androidx.lifecycle.OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                android.util.Log.d("MainActivity", "🔄 Lifecycle: onDestroy")
            }
        })
    }
    
    private fun setupFragmentTransactionLogging() {
        // Add fragment manager logging
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentCreated(fm: androidx.fragment.app.FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
                    android.util.Log.d("MainActivity", "📱 Fragment Created: ${f.javaClass.simpleName}")
                }
                
                override fun onFragmentStarted(fm: androidx.fragment.app.FragmentManager, f: Fragment) {
                    android.util.Log.d("MainActivity", "📱 Fragment Started: ${f.javaClass.simpleName}")
                }
                
                override fun onFragmentResumed(fm: androidx.fragment.app.FragmentManager, f: Fragment) {
                    android.util.Log.d("MainActivity", "📱 Fragment Resumed: ${f.javaClass.simpleName}")
                }
                
                override fun onFragmentPaused(fm: androidx.fragment.app.FragmentManager, f: Fragment) {
                    android.util.Log.d("MainActivity", "📱 Fragment Paused: ${f.javaClass.simpleName}")
                }
                
                override fun onFragmentStopped(fm: androidx.fragment.app.FragmentManager, f: Fragment) {
                    android.util.Log.d("MainActivity", "📱 Fragment Stopped: ${f.javaClass.simpleName}")
                }
                
                override fun onFragmentDestroyed(fm: androidx.fragment.app.FragmentManager, f: Fragment) {
                    android.util.Log.d("MainActivity", "📱 Fragment Destroyed: ${f.javaClass.simpleName}")
                }
                
                override fun onFragmentViewCreated(fm: androidx.fragment.app.FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                    android.util.Log.d("MainActivity", "📱 Fragment View Created: ${f.javaClass.simpleName}")
                }
                
                override fun onFragmentViewDestroyed(fm: androidx.fragment.app.FragmentManager, f: Fragment) {
                    android.util.Log.d("MainActivity", "📱 Fragment View Destroyed: ${f.javaClass.simpleName}")
                }
                
                override fun onFragmentAttached(fm: androidx.fragment.app.FragmentManager, f: Fragment, context: Context) {
                    android.util.Log.d("MainActivity", "📱 Fragment Attached: ${f.javaClass.simpleName} to ${context.javaClass.simpleName}")
                }
                
                override fun onFragmentDetached(fm: androidx.fragment.app.FragmentManager, f: Fragment) {
                    android.util.Log.d("MainActivity", "📱 Fragment Detached: ${f.javaClass.simpleName}")
                }
            }, true
        )
    }
    
    private fun logCrashDetails(throwable: Throwable) {
        try {
            android.util.Log.e("MainActivity", "🚨 CRASH DETAILS:")
            android.util.Log.e("MainActivity", "   Exception Type: ${throwable.javaClass.simpleName}")
            android.util.Log.e("MainActivity", "   Exception Message: ${throwable.message}")
            android.util.Log.e("MainActivity", "   Stack Trace:")
            
            // Log the full stack trace
            throwable.stackTrace.forEach { element ->
                android.util.Log.e("MainActivity", "      at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})")
            }
            
            // Log additional context
            android.util.Log.e("MainActivity", "   Current Activity State: ${if (isFinishing) "Finishing" else "Active"}")
            android.util.Log.e("MainActivity", "   Current Fragment Count: ${supportFragmentManager.fragments.size}")
            android.util.Log.e("MainActivity", "   Current Back Stack Count: ${supportFragmentManager.backStackEntryCount}")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to log crash details", e)
        }
    }
    
    // Enhanced logging for fragment operations
    private fun logFragmentOperation(operation: String, fragment: Fragment?, success: Boolean) {
        try {
            val status = if (success) "✅" else "❌"
            val fragmentName = fragment?.javaClass?.simpleName ?: "Unknown"
            android.util.Log.d("MainActivity", "$status Fragment Operation: $operation - $fragmentName")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to log fragment operation", e)
        }
    }
    
    // Enhanced logging for navigation
    private fun logNavigationAttempt(item: MenuItem) {
        try {
            android.util.Log.d("MainActivity", "🧭 Navigation Attempt:")
            android.util.Log.d("MainActivity", "   Menu Item: ${item.title}")
            android.util.Log.d("MainActivity", "   Menu Item ID: ${item.itemId}")
            android.util.Log.d("MainActivity", "   Current Time: ${java.util.Date()}")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to log navigation attempt", e)
        }
    }

    fun refreshCurrentFragment() {
        try {
            android.util.Log.d("MainActivity", "🔄 refreshCurrentFragment() called")
            
            // Get the current fragment
            val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
            android.util.Log.d("MainActivity", "📱 Current fragment: ${currentFragment?.javaClass?.simpleName}")
            
            // If we're on the account section, refresh it based on current login status
            if (mSelectedMenuItem == R.id.menu_account) {
                android.util.Log.d("MainActivity", "🔄 Refreshing account section based on current login status")
                
                val userInfo = AppManager.getInstance(this).userInfo
                if (userInfo != null) {
                    // User is logged in, show account page
                    android.util.Log.d("MainActivity", "✅ User is logged in, refreshing to SimpleProfileFragment")
                    replaceAccountFragment()
                } else {
                    // User is not logged in, show signin page
                    android.util.Log.d("MainActivity", "ℹ️ User is not logged in, refreshing to SigninFragment")
                    replaceSigninFragment()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Error refreshing current fragment", e)
        }
    }

    fun updateNavigationToAccount() {
        try {
            android.util.Log.d("MainActivity", "🔄 updateNavigationToAccount() called")
            
            // Update the navigation state to account section
            mSelectedMenuItem = R.id.menu_account
            val accountItem = navView.menu.findItem(R.id.menu_account)
            if (accountItem != null) {
                navView.selectedItemId = accountItem.itemId
                android.util.Log.d("MainActivity", "✅ Navigation state updated to account section")
            } else {
                android.util.Log.w("MainActivity", "⚠️ Could not find account menu item")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Error updating navigation to account", e)
        }
    }
}


