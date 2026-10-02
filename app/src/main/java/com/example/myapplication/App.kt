package com.example.myapplication

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthManager.init(this)
        AppSettings.init(this)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(if (AppSettings.darkTheme) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
    }
}
