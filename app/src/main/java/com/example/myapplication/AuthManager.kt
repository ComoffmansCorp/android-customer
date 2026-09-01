package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.network.AuthResponse

object AuthManager {

    private const val PREFS = "auth_prefs"
    private const val KEY_ACCESS_TOKEN  = "accessToken"
    private const val KEY_REFRESH_TOKEN = "refreshToken"
    private const val KEY_FULL_NAME     = "fullName"
    private const val KEY_ROLE          = "role"
    private const val KEY_TENANT        = "tenantName"
    private const val KEY_TENANT_CODE   = "tenantCode"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(v) = prefs.edit().putString(KEY_ACCESS_TOKEN, v).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(v) = prefs.edit().putString(KEY_REFRESH_TOKEN, v).apply()

    var fullName: String?
        get() = prefs.getString(KEY_FULL_NAME, null)
        set(v) = prefs.edit().putString(KEY_FULL_NAME, v).apply()

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(v) = prefs.edit().putString(KEY_ROLE, v).apply()

    var tenantName: String?
        get() = prefs.getString(KEY_TENANT, null)
        set(v) = prefs.edit().putString(KEY_TENANT, v).apply()

    var tenantCode: String?
        get() = prefs.getString(KEY_TENANT_CODE, null)
        set(v) = prefs.edit().putString(KEY_TENANT_CODE, v).apply()

    fun save(response: AuthResponse) {
        accessToken  = response.accessToken
        refreshToken = response.refreshToken
        fullName     = response.fullName
        role         = response.role
        tenantName   = response.tenantName
        tenantCode   = response.tenantCode
    }

    /** Called by the token-refresh authenticator: only the token pair changes. */
    fun saveTokens(newAccessToken: String, newRefreshToken: String) {
        accessToken = newAccessToken
        refreshToken = newRefreshToken
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    val isLoggedIn: Boolean get() = accessToken != null
    val isMaster: Boolean get() = role == "MASTER"
}
