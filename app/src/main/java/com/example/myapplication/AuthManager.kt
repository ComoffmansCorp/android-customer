package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.network.AuthResponse

object AuthManager {

    private const val PREFS = "auth_prefs"
    private const val KEY_TOKEN     = "token"
    private const val KEY_FULL_NAME = "fullName"
    private const val KEY_ROLE      = "role"
    private const val KEY_TENANT    = "tenantName"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(v) = prefs.edit().putString(KEY_TOKEN, v).apply()

    var fullName: String?
        get() = prefs.getString(KEY_FULL_NAME, null)
        set(v) = prefs.edit().putString(KEY_FULL_NAME, v).apply()

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(v) = prefs.edit().putString(KEY_ROLE, v).apply()

    var tenantName: String?
        get() = prefs.getString(KEY_TENANT, null)
        set(v) = prefs.edit().putString(KEY_TENANT, v).apply()

    fun save(response: AuthResponse) {
        token     = response.token
        fullName  = response.fullName
        role      = response.role
        tenantName = response.tenantName
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    val isLoggedIn: Boolean get() = token != null
}
