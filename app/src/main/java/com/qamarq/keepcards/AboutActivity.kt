package com.qamarq.keepcards

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.fragment_home_account.*
import kotlinx.android.synthetic.main.header_navigation_drawer.*
import kotlinx.android.synthetic.main.settings_activity.*


class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        aboutAppBar.setNavigationOnClickListener {
            val i = Intent(this, HomeActivity::class.java)
            startActivity(i)
        }

        version_txt.text = getString(R.string.version, BuildConfig.VERSION_NAME)
    }
}