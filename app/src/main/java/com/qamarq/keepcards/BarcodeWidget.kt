package com.qamarq.keepcards

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import com.google.android.material.elevation.SurfaceColors
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.Writer
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.android.synthetic.main.activity_scan_card.*
import java.util.*


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [BarcodeWidgetConfigureActivity]
 */
class BarcodeWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            deleteTitlePref(context, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val widgetText = loadTitlePref(context, appWidgetId)
    val shopText = loadShopPref(context, appWidgetId)
    val typeCode = loadTypePref(context, appWidgetId)
    val views = RemoteViews(context.packageName, R.layout.barcode_widget)
    val intent = Intent(context, HomeActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.barcodelayout, pendingIntent)
    val typedValue = TypedValue()
    val theme: Resources.Theme = context.theme
    theme.resolveAttribute(com.google.android.material.R.attr.cardForegroundColor, typedValue, true)
    var themeColor = Color.BLACK
    val nightModeFlags: Int = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
    when (nightModeFlags) {
        Configuration.UI_MODE_NIGHT_YES -> themeColor = Color.BLACK
        Configuration.UI_MODE_NIGHT_NO -> themeColor = Color.WHITE
        Configuration.UI_MODE_NIGHT_UNDEFINED -> themeColor = Color.WHITE
    }
    if (typeCode == "barcode") {
        try {
            val productId: String = widgetText
            val hintMap = Hashtable<EncodeHintType, ErrorCorrectionLevel>()
//            val sec_color = getColor(context, com.google.android.material.R.attr.colorSecondary)
            val colorSurface1: Int = SurfaceColors.SURFACE_1.getColor(context)
            hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
            val codeWriter: Writer
            codeWriter = Code128Writer()
            val byteMatrix: BitMatrix =
                codeWriter.encode(productId, BarcodeFormat.CODE_128, 600, 400, hintMap)
            val width = byteMatrix.width
            val height = byteMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (i in 0 until width) {
                for (j in 0 until height) {
                    bitmap.setPixel(i, j, if (byteMatrix[i, j]) Color.BLACK else Color.TRANSPARENT)
                }
            }
            views.setTextViewText(R.id.widget_shop_name, shopText)
            views.setImageViewBitmap(R.id.barcode_img, bitmap)
        } catch (e: Exception) {
            Log.d("fsdfdsfdsfds", "Errrorr")
        }
    } else {
        try {
            val productId: String = widgetText
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(productId, BarcodeFormat.QR_CODE, 400, 400)
            val colorSurface1: Int = SurfaceColors.SURFACE_1.getColor(context)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    pixels[y * w + x] = if (bitMatrix[x, y]) Color.BLACK else Color.TRANSPARENT
                }
            }

            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)

            views.setTextViewText(R.id.widget_shop_name, shopText)
            views.setImageViewBitmap(R.id.barcode_img, bitmap)
        } catch (e: Exception) {
            Log.d("fsdfdsfdsfds", "Errrorr")
        }
    }
    appWidgetManager.updateAppWidget(appWidgetId, views)
}