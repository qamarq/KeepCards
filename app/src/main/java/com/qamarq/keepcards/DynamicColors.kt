package com.qamarq.keepcards

import android.app.Application
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DynamicColors : Application() {
    override fun onCreate() {
        super.onCreate()

        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val offline = settingsPrefs.getBoolean("db_offline", true)
        val dynamically_color = settingsPrefs.getBoolean("dynamic_colors", true)
        val dynamically_theme = settingsPrefs.getString("app_theme", "system")
        if (offline) Firebase.database.setPersistenceEnabled(true)
        if (dynamically_color) DynamicColors.applyToActivitiesIfAvailable(this)

        when (dynamically_theme) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}