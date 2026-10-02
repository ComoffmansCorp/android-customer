package com.example.myapplication

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var navController: NavController
    private val roots = setOf(R.id.availableRequestsFragment, R.id.myRequestsFragment, R.id.masterProfileFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        WindowCompat.getInsetsController(window, b.root).apply {
            isAppearanceLightStatusBars = !AppSettings.darkTheme
            isAppearanceLightNavigationBars = !AppSettings.darkTheme
        }
        setSupportActionBar(b.toolbar)
        navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment).navController
        b.bottomNavigation.setOnItemSelectedListener { item ->
            if (AuthManager.isLoggedIn && navController.currentDestination?.id != item.itemId) {
                navController.navigate(item.itemId, null, NavOptions.Builder()
                    .setPopUpTo(R.id.availableRequestsFragment, false).setLaunchSingleTop(true).build())
            }
            true
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val root = destination.id in roots
            b.bottomNavigation.visibility = if (root && AuthManager.isLoggedIn) View.VISIBLE else View.GONE
            if (root) b.bottomNavigation.menu.findItem(destination.id)?.isChecked = true
            supportActionBar?.title = when (destination.id) {
                R.id.requestDetailFragment -> "Детали заказа"
                R.id.settingsFragment -> "Настройки"
                else -> "мастерская."
            }
            b.toolbar.navigationIcon = if (!root && destination.id != R.id.loginFragment) getDrawable(R.drawable.ic_arrow_left) else null
            b.toolbar.setNavigationOnClickListener { navController.navigateUp() }
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        menu.findItem(R.id.action_theme).apply {
            setIcon(if (AppSettings.darkTheme) R.drawable.ic_sun else R.drawable.ic_moon)
            title = if (AppSettings.darkTheme) "Включить светлую тему" else "Включить тёмную тему"
        }
        menu.findItem(R.id.action_settings).isVisible = navController.currentDestination?.id != R.id.settingsFragment
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_theme -> { toggleTheme(); true }
        R.id.action_settings -> { navController.navigate(R.id.settingsFragment); true }
        else -> super.onOptionsItemSelected(item)
    }

    fun toggleTheme() {
        AppSettings.darkTheme = !AppSettings.darkTheme
        AppCompatDelegate.setDefaultNightMode(if (AppSettings.darkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun logout() {
        AuthManager.clear()
        navController.navigate(R.id.loginFragment, null, NavOptions.Builder().setPopUpTo(navController.graph.id, true).build())
    }

    // Existing fragments call these hooks; primary navigation now belongs to the bottom bar.
    fun setDrawerEnabled(enabled: Boolean) = Unit
    fun setNotificationIconVisible(visible: Boolean) = Unit
    fun setToolbarTitle(title: String) {
        supportActionBar?.title = when (navController.currentDestination?.id) {
            R.id.requestDetailFragment -> "Детали заказа"
            R.id.settingsFragment -> "Настройки"
            else -> "мастерская."
        }
    }
}
