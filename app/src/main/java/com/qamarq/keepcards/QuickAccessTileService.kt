package com.qamarq.keepcards

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class QuickAccessTileService : TileService() {

    override fun onClick() {
        super.onClick()

        if (HomeActivity.floatingFunctions.isOverlay(this)) {
            val intent = Intent(this, LaunchFloatingActivity::class.java)
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
            startActivityAndCollapse(intent)
        } else {
            val intent = Intent(this, RequestFloatingActivity::class.java)
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
            startActivityAndCollapse(intent)
        }
    }
}