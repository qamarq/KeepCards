package com.qamarq.keepcards

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.elevation.SurfaceColors
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_request_floating.*

class RequestFloatingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_floating)

        floatingAppBar.setNavigationOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        checkPerm()
    }

    override fun onResume() {
        super.onResume()
        checkPerm()
    }

    private fun checkPerm() {
        if (HomeActivity.floatingFunctions.isOverlay(this)) {
            req_perm_floating.setText(R.string.f_activated)
            req_perm_floating.isEnabled = false
        } else {
            req_perm_floating.isEnabled = true
            req_perm_floating.setOnClickListener {
                if (HomeActivity.floatingFunctions.isOverlay(this)) {
//                    Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
                } else {
                    requestPermission()
                }
            }
        }
    }

    private fun requestPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }
}