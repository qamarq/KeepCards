package com.qamarq.keepcards

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_request_permission.*


class RequestPermissionActivity : AppCompatActivity() {
    private val MY_CAMERA_REQUEST_CODE = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_permission)

        card1.setOnClickListener {
            if (group1.visibility == View.VISIBLE) {
                TransitionManager.beginDelayedTransition(card1, AutoTransition())
                group1.visibility = View.GONE
                arrow1.setImageResource(android.R.drawable.arrow_down_float)
            } else {
                TransitionManager.beginDelayedTransition(card1, AutoTransition())
                group1.visibility = View.VISIBLE
                arrow1.setImageResource(android.R.drawable.arrow_up_float)
            }
        }

        card2.setOnClickListener {
            if (group2.visibility == View.VISIBLE) {
                TransitionManager.beginDelayedTransition(card2, AutoTransition())
                group2.visibility = View.GONE
                arrow2.setImageResource(android.R.drawable.arrow_down_float)
            } else {
                TransitionManager.beginDelayedTransition(card2, AutoTransition())
                group2.visibility = View.VISIBLE
                arrow2.setImageResource(android.R.drawable.arrow_up_float)
            }
        }

        card3.setOnClickListener {
            if (group3.visibility == View.VISIBLE) {
                TransitionManager.beginDelayedTransition(card3, AutoTransition())
                group3.visibility = View.GONE
                arrow3.setImageResource(android.R.drawable.arrow_down_float)
            } else {
                TransitionManager.beginDelayedTransition(card3, AutoTransition())
                group3.visibility = View.VISIBLE
                arrow3.setImageResource(android.R.drawable.arrow_up_float)
            }
        }

        nextButton.setOnClickListener {
            val i = Intent(this, HomeActivity::class.java)
            startActivity(i)
        }

        skipButton.setOnClickListener {
            // Powiadomienie pyknac
            val i = Intent(this, HomeActivity::class.java)
            startActivity(i)
        }

        val listPerm = arrayOf<String>(android.Manifest.permission.CAMERA)
        photo_permission.setOnClickListener {
            ActivityCompat.requestPermissions(this, listPerm, MY_CAMERA_REQUEST_CODE)
        }

        settings_permission.setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        showontop_permission.setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        checkDonePermissions()
    }

    override fun onResume() {
        super.onResume()
        checkDonePermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_CAMERA_REQUEST_CODE -> {
                checkDonePermissions()
            }
        }
    }

    private fun checkDonePermissions() {
        nextButton.isEnabled = false
        group1.visibility = View.GONE
        arrow1.setImageResource(android.R.drawable.arrow_down_float)
        group2.visibility = View.GONE
        arrow2.setImageResource(android.R.drawable.arrow_down_float)
        group3.visibility = View.GONE
        arrow3.setImageResource(android.R.drawable.arrow_down_float)

        var steps = 0

        // Camera
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            card1.isEnabled = false
            img1.setImageResource(R.drawable.ic_baseline_done_24)
            arrow1.visibility = View.GONE
            steps += 1
        }

        // Brightness
        if (Settings.System.canWrite(this)) {
            card2.isEnabled = false
            img2.setImageResource(R.drawable.ic_baseline_done_24)
            arrow2.visibility = View.GONE
            steps += 1
        }

        // Show on top
        if (HomeActivity.floatingFunctions.isOverlay(this)) {
            card3.isEnabled = false
            img3.setImageResource(R.drawable.ic_baseline_done_24)
            arrow3.visibility = View.GONE
            steps += 1
        }

        if (steps == 3) {
            nextButton.isEnabled = true
        }
    }
}