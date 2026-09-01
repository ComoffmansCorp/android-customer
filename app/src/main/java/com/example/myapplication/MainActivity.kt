package com.example.myapplication

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var b: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var toggle: ActionBarDrawerToggle
    private var notificationMenuItem: MenuItem? = null

    private val roleLabels = mapOf(
        "SUPER_ADMIN" to "Супер-администратор",
        "TENANT_ADMIN" to "Администратор",
        "DISPATCHER" to "Диспетчер",
        "ELECTRICIAN" to "Инспектор",
        "MASTER" to "Мастер"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        toggle = ActionBarDrawerToggle(
            this, b.drawerLayout, b.toolbar,
            R.string.app_name, R.string.app_name
        )
        b.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = getColor(R.color.dark)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHost.navController

        b.navView.setNavigationItemSelectedListener(this)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    toggle.isDrawerIndicatorEnabled = false
                    b.toolbar.navigationIcon = null
                    invalidateOptionsMenu()
                }
                else -> {
                    toggle.isDrawerIndicatorEnabled = true
                    toggle.syncState()
                    invalidateOptionsMenu()
                    updateNavHeader()
                    applyRoleMenu()
                }
            }
        }
    }

    /**
     * The B2B (electrician) and marketplace (master) drawer sections are
     * mutually exclusive. MASTER also gets the "Мастерская" indigo accent
     * on its own menu items instead of the B2B brass one -- same identity
     * split as the web (MarketplaceShell.vue vs AppShell.vue).
     */
    private fun applyRoleMenu() {
        val menu = b.navView.menu
        val isMaster = AuthManager.isMaster
        listOf(R.id.nav_tasks, R.id.nav_acts, R.id.nav_refs).forEach {
            menu.findItem(it)?.isVisible = !isMaster
        }
        listOf(R.id.nav_available_requests, R.id.nav_my_requests, R.id.nav_master_profile).forEach {
            menu.findItem(it)?.isVisible = isMaster
        }
        b.navView.itemIconTintList = android.content.res.ColorStateList.valueOf(
            getColor(if (isMaster) R.color.mk_accent else R.color.brand)
        )
    }

    private fun updateNavHeader() {
        val header = b.navView.getHeaderView(0) ?: return
        header.findViewById<android.widget.TextView>(R.id.nav_name)?.text =
            AuthManager.fullName ?: getString(R.string.app_name)
        header.findViewById<android.widget.TextView>(R.id.nav_role)?.text =
            AuthManager.role?.let { roleLabels[it] ?: it } ?: ""
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        notificationMenuItem = menu.findItem(R.id.action_notifications)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        b.drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_tasks -> {
                if (navController.currentDestination?.id != R.id.taskListFragment)
                    navController.navigate(R.id.taskListFragment)
            }
            R.id.nav_refs -> {
                if (navController.currentDestination?.id != R.id.dictionaryFragment)
                    navController.navigate(R.id.dictionaryFragment)
            }
            R.id.nav_acts -> {
                if (navController.currentDestination?.id != R.id.myActsFragment)
                    navController.navigate(R.id.myActsFragment)
            }
            R.id.nav_settings -> {
                if (navController.currentDestination?.id != R.id.settingsFragment)
                    navController.navigate(R.id.settingsFragment)
            }
            R.id.nav_available_requests -> {
                if (navController.currentDestination?.id != R.id.availableRequestsFragment)
                    navController.navigate(R.id.availableRequestsFragment)
            }
            R.id.nav_my_requests -> {
                if (navController.currentDestination?.id != R.id.myRequestsFragment)
                    navController.navigate(R.id.myRequestsFragment)
            }
            R.id.nav_master_profile -> {
                if (navController.currentDestination?.id != R.id.masterProfileFragment)
                    navController.navigate(R.id.masterProfileFragment)
            }
            R.id.nav_logout   -> {
                AuthManager.clear()
                navController.navigate(R.id.loginFragment)
            }
        }
        return true
    }

    override fun onBackPressed() {
        if (b.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            b.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    fun setDrawerEnabled(enabled: Boolean) {
        toggle.isDrawerIndicatorEnabled = enabled
        if (enabled) {
            b.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            b.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
        toggle.syncState()
    }

    fun setNotificationIconVisible(visible: Boolean) {
        notificationMenuItem?.isVisible = visible
    }
}
