package com.qamarq.keepcards

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings

class LaunchFloatingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_floating)

        if (HomeActivity.floatingFunctions.isOverlay(this)) {
            if (isServiceRunning()) {
                stopService(Intent(this, FloatingCards::class.java))
            }
            startService(Intent(this, FloatingCards::class.java))
            finish()
        } else {
            requestPermission()
        }
    }

    private fun isServiceRunning() : Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        manager.getRunningServices(Int.MAX_VALUE).forEach { service ->
            if (FloatingCards::class.java.name == service.service.className) return true
        }
        return false
    }

    private fun requestPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }
}