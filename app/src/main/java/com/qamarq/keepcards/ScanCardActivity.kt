package com.qamarq.keepcards

import android.content.*
import android.content.res.Resources.Theme
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.ktx.storage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.Writer
import com.google.zxing.common.BitMatrix
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.onesignal.OneSignal
import kotlinx.android.synthetic.main.activity_add.*
import kotlinx.android.synthetic.main.activity_scan_card.*
import kotlinx.android.synthetic.main.activity_scan_card.client_id_txt
import kotlinx.android.synthetic.main.activity_scan_card.shop_name_txt
import kotlinx.android.synthetic.main.fragment_home_cards.*
import kotlinx.android.synthetic.main.fragment_home_friends.*
import kotlinx.android.synthetic.main.share_bottom_sheet.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.concurrent.schedule


class ScanCardActivity : AppCompatActivity() {
    private val sharedPrefFile = "keepcardspref"
    private var globalDialog: BottomSheetDialog? = null
    var defaultBrightness = 0
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val i = Intent(this@ScanCardActivity, HomeActivity::class.java)
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_card)
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val database = Firebase.database.reference
        val curr_shop = sharedPreferences.getString("curr_shop","").toString()
        val curr_type = sharedPreferences.getString("curr_type","").toString()
        val curr_clientid = sharedPreferences.getString("curr_clientid","").toString()

        manageBrightness()

        scanTopAppBar.title = curr_shop.capitalize()
        shop_title.text = curr_shop.capitalize()
        client_id_desc.text = "Client ID: $curr_clientid"
        scanTopAppBar.setNavigationOnClickListener {
            restoreBrightness()
            val i = Intent(this@ScanCardActivity, HomeActivity::class.java)
            startActivity(i)
        }

        scanTopAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.delete -> {
                    val userId = Firebase.auth.currentUser?.uid
                    val deleteQuery: Query = database.child("users").child(userId.toString()).child("cards").child(curr_clientid)
                    deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (deleteSnapshot in dataSnapshot.children) {
                                deleteSnapshot.ref.removeValue()
//                                Toast.makeText(this@ScanCardActivity, R.string.delete_success, Toast.LENGTH_SHORT).show()
                                restoreBrightness()
                                val i = Intent(this@ScanCardActivity, HomeActivity::class.java)
                                startActivity(i)
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {

                        }
                    })
                    true
                }
                R.id.copy -> {
                    setClipboard(this, curr_clientid)
                    Toast.makeText(this, R.string.success_copy, Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.share -> {
                    val bottomSheetDialog = BottomSheetDialog(this@ScanCardActivity, R.style.BottomSheetDialogTheme)
                    val bottomSheetView = LayoutInflater.from(applicationContext).inflate(
                        R.layout.share_bottom_sheet,
                        findViewById<LinearLayout>(R.id.bottomSheet)
                    )
                    globalDialog = bottomSheetDialog
                    bottomSheetView.findViewById<View>(R.id.sendAddCardLink)?.setOnClickListener {
                        val sendShop = sharedPreferences.getString("curr_shop","").toString()
                        val sendClientId = sharedPreferences.getString("curr_clientid","").toString()
                        val cardType = sharedPreferences.getString("curr_type","").toString()
                        var linkSendShop = sendShop.replace(" ","+")
                        val link =
                            "https://keepcards.page.link/?link=https://keepcards-qamarq.firebaseapp.com/?type%3Dadd_card%26shop_name%3D$linkSendShop%26clientId%3D$sendClientId%26cardType%3D$cardType&apn=com.qamarq.keepcards"
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.link_add_card_send, sendShop, link))
                        startActivity(Intent.createChooser(intent, "Share Link"))
                    }
                    bottomSheetDialog.setContentView(bottomSheetView)
                    bottomSheetDialog.show()
                    loadFriendsToDialog()
                    true
                }
                else -> false
            }
        }

        val colorSurface1: Int = SurfaceColors.SURFACE_5.getColor(this)
//        val primaryColor1: Int = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary)
        val typedValue = TypedValue()
        val theme: Theme = this.theme

        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        @ColorInt val primaryColor = typedValue.data

        theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true)
        @ColorInt val backgroundColor = typedValue.data
        if (curr_type == "barcode") {
            try {
                val productId: String = curr_clientid
                val hintMap = Hashtable<EncodeHintType, ErrorCorrectionLevel>()
                hintMap[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.L
                val codeWriter: Writer
                codeWriter = Code128Writer()
                val byteMatrix: BitMatrix =
                    codeWriter.encode(productId, BarcodeFormat.CODE_128, 600, 200, hintMap)
                val width = byteMatrix.width
                val height = byteMatrix.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                for (i in 0 until width) {
                    for (j in 0 until height) {
                        bitmap.setPixel(i, j, if (byteMatrix[i, j]) primaryColor else backgroundColor)
                    }
                }
                scan_img.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
            }
        } else {
            try {
                scan_img.layoutParams.height = 800
                scan_img.requestLayout()
                val productId: String = curr_clientid
                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(productId, BarcodeFormat.QR_CODE, 400, 400)

                val w = bitMatrix.width
                val h = bitMatrix.height
                val pixels = IntArray(w * h)
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        pixels[y * w + x] = if (bitMatrix[x, y]) primaryColor else backgroundColor
                    }
                }

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, w, 0, 0, w, h)

                scan_img.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.d("fsdfdsfdsfds", "Errrorr")
            }
        }

        find_store.setOnClickListener {
            val gmmIntentUri =
                Uri.parse("geo:52.4221811,20.2655489?q=" + Uri.encode(curr_shop))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        fullsreen_code.setOnClickListener { openDialog() }

        copy_clientid.setOnClickListener {
            setClipboard(this, curr_clientid)
            Toast.makeText(this, R.string.success_copy, Toast.LENGTH_SHORT).show()
        }

        scan_changetype.addOnButtonCheckedListener { toggleButtonGroup, checkedId, isChecked ->
            var selectedType = "barcode"
            if (isChecked) {
                when (checkedId) {
                    R.id.barcode_segment -> {
                        selectedType = "barcode"
                    }
                    R.id.qrcode_segment -> {
                        selectedType = "qrcode"
                    }
                }
            } else {
                if (toggleButtonGroup.checkedButtonId == View.NO_ID) {}
            }
            val user = Firebase.auth.currentUser
            val userId = user?.uid
            database.child("users")
                .child(userId.toString())
                .child("cards")
                .child(curr_clientid)
                .child("type")
                .setValue(selectedType)
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString("curr_type",selectedType)
            editor.apply()
            editor.commit()
            val i = Intent(this@ScanCardActivity, ScanCardActivity::class.java)
            startActivity(i)
        }

        changeShopName.setOnClickListener {
            val shopName = shop_name_txt.text.toString()
            if (shopName.isNotEmpty()) {
                val user = Firebase.auth.currentUser
                val userId = user?.uid
                database.child("users")
                    .child(userId.toString())
                    .child("cards")
                    .child(curr_clientid)
                    .child("shop")
                    .setValue(shopName)
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putString("curr_shop",shopName)
                editor.apply()
                editor.commit()
                val i = Intent(this@ScanCardActivity, ScanCardActivity::class.java)
                startActivity(i)
            } else {
                Toast.makeText(this, R.string.complete_text, Toast.LENGTH_LONG).show()
            }
        }

        changeClientID.setOnClickListener {
            val newClientID = client_id_txt.text.toString()
            if (newClientID.isNotEmpty()) {
                val user = Firebase.auth.currentUser
                val userId = user?.uid
                val deleteQuery: Query = database.child("users").child(userId.toString()).child("cards").child(curr_clientid)
                deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (deleteSnapshot in dataSnapshot.children) {
                            deleteSnapshot.ref.removeValue()
                            database
                                .child("users")
                                .child(userId.toString())
                                .child("cards")
                                .child(newClientID).setValue(HomeActivity.newCard(newClientID, curr_type, curr_shop))
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putString("curr_clientid",newClientID)
                            editor.apply()
                            Toast.makeText(this@ScanCardActivity, R.string.success_change_client, Toast.LENGTH_SHORT).show()
                            val i = Intent(this@ScanCardActivity, ScanCardActivity::class.java)
                            startActivity(i)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
            } else {
                Toast.makeText(this, R.string.complete_text, Toast.LENGTH_LONG).show()
            }
        }

        changeAll.setOnClickListener {
            val shopName = shop_name_txt.text.toString()
            val newClientID = client_id_txt.text.toString()
            if (newClientID.isNotEmpty() && shopName.isNotEmpty()) {
                val user = Firebase.auth.currentUser
                val userId = user?.uid
                val deleteQuery: Query = database.child("users").child(userId.toString()).child("cards").child(curr_clientid)
                deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (deleteSnapshot in dataSnapshot.children) {
                            deleteSnapshot.ref.removeValue()
                            database
                                .child("users")
                                .child(userId.toString())
                                .child("cards")
                                .child(newClientID).setValue(HomeActivity.newCard(newClientID, curr_type, shopName))
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putString("curr_clientid",newClientID)
                            editor.putString("curr_shop",shopName)
                            editor.apply()
                            editor.commit()
                            val i = Intent(this@ScanCardActivity, ScanCardActivity::class.java)
                            startActivity(i)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {

                    }
                })
            } else {
                Toast.makeText(this, R.string.complete_text, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun manageBrightness() {
        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val brightness = settingsPrefs.getBoolean("brightness", true)
        if (brightness) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.permission_dialog_title)
                        .setMessage(R.string.permission_dialog_desc)
                        .setPositiveButton(resources.getString(R.string.dialog_dynamic_btn)) { dialog, _ ->
                            dialog.cancel()
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = Uri.parse("package:$packageName")
                            startActivity(intent)
                        }
                        .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                            dialog.cancel()
                        }
                        .show()
                } else {
                    val cResolver = contentResolver
                    defaultBrightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS)
                    Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 255)
                }
            } else {
                val cResolver = contentResolver
                defaultBrightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS)
                Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 255)
            }
        }
    }

    private fun restoreBrightness() {
        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val brightness = settingsPrefs.getBoolean("brightness", true)
        if (brightness) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {
                    val cResolver = contentResolver
                    Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, defaultBrightness)
                }
            } else {
                val cResolver = contentResolver
                Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, defaultBrightness)
            }
        }
    }

    private fun loadFriendsToDialog() {
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val database = Firebase.database.reference
        val userId = Firebase.auth.currentUser?.uid

        database.child("users").child(userId.toString()).child("friends").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var emptyData: Boolean = true
                globalDialog?.horizontalScroll_linear?.removeAllViews()
                var lastCard: LinearLayout? = null
                dataSnapshot.children.forEach {
                    emptyData = false
                    val name = it.child("name").value.toString()
                    val friendId = it.key.toString()
                    database.child("users").child(friendId).child("notify_id").get().addOnSuccessListener { data ->
                        val friendNotifyId = data.value
                        val card = LinearLayout(this@ScanCardActivity)
                        lastCard = card
                        card.orientation = LinearLayout.VERTICAL
                        val gravityPar: LinearLayout.LayoutParams = LinearLayout.LayoutParams(310,450)
                        gravityPar.gravity = Gravity.CENTER
                        card.layoutParams = gravityPar
                        val param = card.layoutParams as ViewGroup.MarginLayoutParams
                        param.setMargins(15,5,15,5)
                        card.layoutParams = param

                        card.setOnClickListener {
                            globalDialog?.dismiss()
                            val curr_shop = sharedPreferences.getString("curr_shop","").toString()
                            val curr_type = sharedPreferences.getString("curr_type","").toString()
                            val curr_clientid = sharedPreferences.getString("curr_clientid","").toString()
                            database
                                .child("users")
                                .child(friendId)
                                .child("cards_request")
                                .child(curr_clientid).setValue(HomeActivity.newCard(curr_clientid, curr_type, curr_shop))
                            OneSignal.postNotification(
                                JSONObject("{'contents': {'en':'${getString(R.string.notification_new_card)}'}, 'include_player_ids': ['$friendNotifyId']}"),
                                object : OneSignal.PostNotificationResponseHandler {
                                    override fun onSuccess(response: JSONObject) {
                                        Log.i("OneSignalExample", "postNotification Success: $response")
                                    }

                                    override fun onFailure(response: JSONObject) {
                                        Log.e("OneSignalExample", "postNotification Failure: $response")
                                    }
                                })
                            Toast.makeText(this@ScanCardActivity, getString(R.string.card_add_req_send, name), Toast.LENGTH_LONG).show()
                        }

                        val titleText = TextView(this@ScanCardActivity)
                        titleText.text = name
                        titleText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)
                        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            160
                        )
                        params.setMargins(0, 0, 0, 5)
                        titleText.layoutParams = params
                        titleText.textAlignment = View.TEXT_ALIGNMENT_CENTER

                        val imgView = ImageView(this@ScanCardActivity)
                        val params2 = LinearLayout.LayoutParams(250, 250)
                        params2.setMargins(30, 0, 0, 0)
                        params2.weight = 1.0f
                        imgView.setPadding(10, 10, 10, 0)
                        imgView.layoutParams = params2

                        val storage = Firebase.storage
                        var my_image: Bitmap?
                        val ref = storage.reference.child("profiles/$friendId/avatar.jpg")
                        try {
                            val localFile: File = File.createTempFile("Images", "bmp")
                            ref.getFile(localFile)
                                .addOnSuccessListener(OnSuccessListener<FileDownloadTask.TaskSnapshot?> {
                                    my_image = BitmapFactory.decodeFile(localFile.getAbsolutePath())
                                    my_image?.circularIt(applicationContext)?.apply {
                                        imgView.setImageDrawable(this)
                                    }
                                }).addOnFailureListener(
                                    OnFailureListener { e ->
                                        imgView.setImageResource(R.drawable.ic_baseline_no_accounts_24)
                                    })
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                        card.addView(imgView)
                        card.addView(titleText)
                        globalDialog?.horizontalScroll_linear?.addView(card)
                    }
                }
//                if (!emptyData) {
//                    val param = lastCard?.layoutParams as ViewGroup.MarginLayoutParams
//                    param.setMargins(15,5,95,5)
//                    lastCard?.layoutParams ?: params
//                }
                if (emptyData) {
                    globalDialog?.horizontalScroll?.visibility = View.GONE
                    globalDialog?.space?.visibility = View.GONE
                    val param = globalDialog?.titleBottomDialog?.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,50,50,50)
                    globalDialog?.titleBottomDialog?.layoutParams ?: params
                } else {
                    Timer("margincard", false).schedule(1000) {
                        if (lastCard != null) {
                            val param = lastCard?.layoutParams as ViewGroup.MarginLayoutParams
                            param.setMargins(15,5,95,5)
                            lastCard?.layoutParams ?: params
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.message) //Never ignore potential errors!
            }
        })
    }

    fun Bitmap.circularIt(
        context: Context,
        diameter:Int = Integer.min(width, height)
    ): RoundedBitmapDrawable? {
        // Calculate the bitmap diameter and radius
        val length = Integer.min(diameter, Integer.min(width, height))
        val radius = length / 2F

        val bitmap = Bitmap.createBitmap(length,length,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // draw bitmap at canvas center
        canvas.drawBitmap(
            this, // bitmap
            (diameter - this.width)/2F, // left
            (diameter - this.height)/2F, // top
            null // paint
        )

        // return the rounded bitmap drawable as circle shape
        return RoundedBitmapDrawableFactory.create(context.resources,bitmap).apply {
            isCircular = true
            setAntiAlias(true)
        }
    }


    private fun setClipboard(context: Context, text: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            val clipboard =
                context.getSystemService(CLIPBOARD_SERVICE) as android.text.ClipboardManager
            clipboard.text = text
        } else {
            val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
        }
    }

    private fun openDialog() {
        FullscreenDialog.display(supportFragmentManager);
    }
}