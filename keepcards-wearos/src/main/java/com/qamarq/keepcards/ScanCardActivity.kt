package com.qamarq.keepcards

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import androidx.core.view.MotionEventCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.oned.Code128Writer
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.android.synthetic.main.activity_scan_card.*
import java.io.Writer
import java.util.*

class ScanCardActivity : AppCompatActivity() {
    private val sharedPrefFile = "keepcardspref"
    var defaultBrightness = 0
    override fun onBackPressed() {
        restoreBrightness()
        val i = Intent(this@ScanCardActivity, MainActivity::class.java)
        startActivity(i)
    }
    override fun onPause() {
        super.onPause()
        restoreBrightness()
    }
    override fun onResume() {
        super.onResume()
        manageBrightness()
    }
    override fun onStop() {
        super.onStop()
        restoreBrightness()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_card)
        val curr_type = sharedPreferences.getString("curr_type","").toString()
        val productId = sharedPreferences.getString("curr_clientid","").toString()
        val curr_shop = sharedPreferences.getString("curr_shop","").toString()
        manageBrightness()

        if (curr_type == "barcode") {
            try {
                val hintMap = Hashtable<EncodeHintType, ErrorCorrectionLevel>()
                hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
                val codeWriter: com.google.zxing.Writer
                codeWriter = Code128Writer()
                val byteMatrix: BitMatrix =
                    codeWriter.encode(productId, BarcodeFormat.CODE_128, 800, 300, hintMap)
                val width = byteMatrix.width
                val height = byteMatrix.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                for (i in 0 until width) {
                    for (j in 0 until height) {
                        bitmap.setPixel(i, j, if (byteMatrix[i, j]) Color.BLACK else Color.WHITE)
                    }
                }
//                shopName.text = curr_shop
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.d("fsdfdsfdsfds", "Errrorr")
            }
        } else {
            try {
                imageView.layoutParams.height = 800
                imageView.requestLayout()
                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(productId, BarcodeFormat.QR_CODE, 400, 400)

                val w = bitMatrix.width
                val h = bitMatrix.height
                val pixels = IntArray(w * h)
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        pixels[y * w + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    }
                }

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, w, 0, 0, w, h)

                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.d("fsdfdsfdsfds", "Errrorr")
            }
        }
    }

    private fun manageBrightness() {
        if (!Settings.System.canWrite(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_desc)
                .setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
                    dialog.cancel()
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
        } else {
            val cResolver = contentResolver
            defaultBrightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS)
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 255)
        }
    }

    private fun restoreBrightness() {
        if (Settings.System.canWrite(this)) {
            val cResolver = contentResolver
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, defaultBrightness)
        }
    }
}