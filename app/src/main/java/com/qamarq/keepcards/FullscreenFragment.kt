package com.qamarq.keepcards

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.Writer
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.android.synthetic.main.activity_scan_card.*
import kotlinx.android.synthetic.main.fragment_fullscreen.*
import java.util.*


class FullscreenDialog : DialogFragment() {
    private var toolbar: Toolbar? = null
    private val sharedPrefFile = "keepcardspref"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)

            val preferences = this.activity!!
                .getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
            val curr_clientid = preferences.getString("curr_clientid","").toString()
            val curr_type = preferences.getString("curr_type","").toString()
            if (curr_type == "barcode") {
                try {
                    val productId: String = curr_clientid
                    val hintMap = Hashtable<EncodeHintType, ErrorCorrectionLevel>()
                    hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
                    val codeWriter: Writer
                    codeWriter = Code128Writer()
                    val byteMatrix: BitMatrix =
                        codeWriter.encode(productId, BarcodeFormat.CODE_128, 400, 200, hintMap)
                    val width = byteMatrix.width
                    val height = byteMatrix.height
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    for (i in 0 until width) {
                        for (j in 0 until height) {
                            bitmap.setPixel(i, j, if (byteMatrix[i, j]) Color.BLACK else Color.WHITE)
                        }
                    }
                    imageView1.setImageBitmap(bitmap)
                    imageView2.setImageBitmap(bitmap)
                } catch (e: Exception) {

                }
            } else {
                try {
                    imageView1.layoutParams.height = 800
                    imageView1.requestLayout()
                    imageView2.visibility = View.GONE
                    val productId: String = curr_clientid
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

                    imageView1.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.d("fsdfdsfdsfds", "Errrorr")
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view: View = inflater.inflate(R.layout.fragment_fullscreen, container, false)
        toolbar = view.findViewById(R.id.toolbar)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val preferences = this.activity!!
            .getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
        val shop = preferences.getString("curr_shop","").toString()
        super.onViewCreated(view, savedInstanceState)
        toolbar!!.setNavigationOnClickListener { dismiss() }
        toolbar!!.title = shop.capitalize()
        toolbar!!.inflateMenu(R.menu.fullscreen_dialog)
        toolbar!!.setOnMenuItemClickListener {
            dismiss()
            true
        }
    }

    companion object {
        const val TAG = "example_dialog"
        fun display(fragmentManager: FragmentManager?): FullscreenDialog {
            val FullscreenDialog = FullscreenDialog()
            FullscreenDialog.show(fragmentManager!!, TAG)
            return FullscreenDialog
        }
    }
}