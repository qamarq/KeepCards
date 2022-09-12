package com.qamarq.keepcards

import android.app.Application
import com.google.android.material.color.DynamicColors

class DynamicColors : Application() {
    companion object {
        var globalUsername: String = ""
        var globalEmail: String = ""
        var globalPass: String = ""
    }
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}