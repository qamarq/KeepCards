package com.qamarq.keepcards

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.OnLongClickListener
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.ktx.storage
import com.onesignal.OneSignal
import kotlinx.android.synthetic.main.activity_add.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.topAppBar
import kotlinx.android.synthetic.main.fragment_home_account.*
import kotlinx.android.synthetic.main.fragment_home_cards.*
import kotlinx.android.synthetic.main.fragment_home_friends.*
import kotlinx.android.synthetic.main.header_navigation_drawer.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.Integer.min
import java.util.concurrent.Executor


class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val sharedPrefFile = "keepcardspref"
    private val pickImage = 100
    private val UPDATE_REQ_CODE = 100
    private var mAppUpdateManager: AppUpdateManager? = null
    private val RC_APP_UPDATE = 11
    var globalCardsList: MutableList<MutableList<String>> = mutableListOf<MutableList<String>>()
    private var imageUri: Uri? = null

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Log.d("KeepC", "Not allow")
    }
    private val ONESIGNAL_APP_ID = "16bb382f-8259-4af8-b862-95697361a97e"
    @IgnoreExtraProperties
    data class newFriend(val name: String? = null, val email: String? = null) {}
    data class newCard(val clientid: String? = null, val type: String? = null, val shop: String? = null) {}

    private val database = Firebase.database.reference
    private var cardReady: Boolean = false

    private var biometricAvailable = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // Check if the initial data is ready.
                    return if (cardReady) {
                        // The content is ready; start drawing.
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        // The content is not ready; suspend.
                        false
                    }
                }
            }
        )

        checkDeviceHasBiometric()
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    val contextView = findViewById<View>(android.R.id.content)
                    if (errString == resources.getString(R.string.cancel)) {
                        Snackbar.make(contextView, "Anulowano", Snackbar.LENGTH_SHORT)
                            .setAnchorView(extended_fab)
                            .show()
                    } else {
                        Snackbar.make(contextView, "$errString", Snackbar.LENGTH_SHORT)
                            .setAnchorView(extended_fab)
                            .show()
                    }
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    val i = Intent(this@HomeActivity, ScanCardActivity::class.java)
                    startActivity(i)
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Potwierdź swoją tożsamość")
            .setSubtitle("Aby otworzyć Bezpieczną Kartę")
            .setNegativeButtonText(resources.getString(R.string.cancel))
            .build()

        var fabActualMode = "home"

        auth = Firebase.auth

        val manager = ReviewManagerFactory.create(this)
        val userId = Firebase.auth.currentUser?.uid

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
        OneSignal.initWithContext(this)
        OneSignal.setAppId(ONESIGNAL_APP_ID)
        OneSignal.setExternalUserId(userId.toString())
        val deviceState = OneSignal.getDeviceState()
        val onesignalUserId = deviceState?.userId
        database
            .child("users")
            .child(userId.toString())
            .child("notify_id").setValue(onesignalUserId)
        extended_fab.setOnClickListener {
            when (fabActualMode) {
                "home" -> {
                    val i = Intent(this@HomeActivity, AddActivity::class.java)
                    startActivity(i)
                }
                "friends" -> {
                    SearchFriendFragment.display(supportFragmentManager)
                }
                "account" -> {
        //                saveProfileData()
                }
            }
        }

        topAppBar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            drawerLayout.close()
            when (menuItem.itemId) {
//                R.id.floating -> {
//                    val i = Intent(this@HomeActivity, RequestFloatingActivity::class.java)
//                    startActivity(i)
//                }
                R.id.archive -> {
                    val i = Intent(this@HomeActivity, ArchiveActivity::class.java)
                    startActivity(i)
                }
                R.id.settings -> {
                    val i = Intent(this@HomeActivity, SettingsActivity::class.java)
                    startActivity(i)
                }
                R.id.about -> {
                    val i = Intent(this@HomeActivity, AboutActivity::class.java)
                    startActivity(i)
                }
                R.id.logout -> {
                    Firebase.auth.signOut()
                    val i = Intent(this@HomeActivity, MainActivity::class.java)
                    startActivity(i)
                    true
                }
                else -> false
            }
            true
        }

        profile_logout_btn.setOnClickListener {
            Firebase.auth.signOut()
            val i = Intent(this@HomeActivity, MainActivity::class.java)
            startActivity(i)
        }

        profile_delete_btn.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.delete_account_title))
                .setMessage(resources.getString(R.string.delete_account_desc))
                .setCancelable(false)
                .setPositiveButton(resources.getString(R.string.dialog_dynamic_btn)) { _, _ ->
                    Firebase.auth.signOut()
                    val i = Intent(this@HomeActivity, MainActivity::class.java)
                    startActivity(i)
                }
                .setNeutralButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setIcon(R.drawable.ic_report_fill1_wght400_grad0_opsz48)
                .show()
        }

        nest_scrollview.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY + 12 && extended_fab.isExtended) {
                extended_fab.shrink()
            }
            if (scrollY < oldScrollY - 12 && !extended_fab.isExtended) {
                extended_fab.extend()
            }
            if (scrollY == 0) {
                extended_fab.extend()
            }
        }

        val vf = findViewById<View>(R.id.vf) as ViewFlipper
        bottom_navigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.home -> {
                    vf.displayedChild = 3
                    fabActualMode = "home"
                    topAppBar.setTitle(R.string.title_home)
                    extended_fab.setText(R.string.fab_add)
                    if (!extended_fab.isVisible) {
                        extended_fab.show()
                    }
                    true
                }
                R.id.friends -> {
                    vf.displayedChild = 1
                    fabActualMode = "friends"
                    topAppBar.setTitle(R.string.title_friends)
                    extended_fab.setText(R.string.fab_friend)
                    if (!extended_fab.isVisible) {
                        extended_fab.show()
                    }
                    true
                }
                R.id.account -> {
                    vf.displayedChild = 2
                    fabActualMode = "account"
                    extended_fab.hide()
                    topAppBar.setTitle(R.string.title_account)
                    extended_fab.setText(R.string.fab_account)
                    true
                }
                else -> false
            }
        }
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val myEmail = sharedPreferences.getString("globalEmail","").toString()
        database.child("users").child(userId.toString()).child("username").get().addOnSuccessListener {
            val user = Firebase.auth.currentUser!!
            val editor: SharedPreferences.Editor =  sharedPreferences.edit()
            header_title.text = "${it.value}"
            profile_name.text = it.value as CharSequence?
            profile_email.text = myEmail
            editor.putString("globalUsername","${it.value}")
            var googleLoged = false
            var emailLoged = false
            loop@ for (userInfo in user.providerData) {
                when (userInfo.providerId) {
                    EmailAuthProvider.PROVIDER_ID -> emailLoged = true
                    GoogleAuthProvider.PROVIDER_ID -> googleLoged = true
                }
            }
            if (!emailLoged) email_chip.visibility = View.GONE
            if (!googleLoged) google_chip.visibility = View.GONE
            editor.apply()
            header_email.text = myEmail
            if (it.value == null) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(resources.getString(R.string.dialog_null_title))
                    .setMessage(resources.getString(R.string.dialog_null_desc))
                    .setCancelable(false)
                    .setPositiveButton(resources.getString(R.string.dialog_dynamic_btn)) { dialog, _ ->
                        user.delete()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    dialog.cancel()
                                    val i = Intent(this@HomeActivity, MainActivity::class.java)
                                    startActivity(i)
                                } else {
                                    Firebase.auth.signOut()
                                    val i = Intent(this@HomeActivity, MainActivity::class.java)
                                    startActivity(i)
                                }
                            }
                    }
                    .show()
            }
        }.addOnFailureListener{

        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.more -> {
                    Firebase.auth.signOut()
                    val i = Intent(this@HomeActivity, MainActivity::class.java)
                    startActivity(i)
                    true
                }
                R.id.review -> {
                    val request = manager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // We got the ReviewInfo object
                            val reviewInfo = task.result
                            val flow = manager.launchReviewFlow(this, reviewInfo)
                            flow.addOnCompleteListener { _ ->
                                Toast.makeText(this, "Thx for review", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // There was some problem, log or handle the error code.
//                            @ReviewErrorCode val reviewErrorCode = (task.getException() as TaskException).errorCode
                        }
                    }
                    true
                }
                else -> false
            }
        }
//        wearos_fab.setOnClickListener {
//            MaterialAlertDialogBuilder(this)
//                .setTitle(resources.getString(R.string.sync))
//                .setMessage(resources.getString(R.string.dialog_sync_wearos_desc))
//                .setPositiveButton(resources.getString(R.string.next)) { dialog, _ ->
//                    dialog.cancel()
//                    sendCardsToWatch()
//                }
//                .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
//                    dialog.cancel()
//                }
//                .show()
//        }

        makeFriendsLayout()
        makeCardLayout()
        profileFragementCode()

        checkBrightnessPermission()

        inviteListener()
        newCardListener()
        checkLinkInvitations()
        checkLinkAddCard()
    }

    private fun checkDeviceHasBiometric() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricAvailable = true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                biometricAvailable = false

            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                biometricAvailable = false

            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                    }
                    startActivityForResult(enrollIntent, 100)
                }

                biometricAvailable = false
            }
        }
    }

    private fun setClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

    object floatingFunctions {
        fun isOverlay(context: Context) : Boolean {
            return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) Settings.canDrawOverlays(context)
            else return true
        }
    }

    private fun checkBrightnessPermission() {
//        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
//        val brightness = settingsPrefs.getBoolean("brightness", true)
//        if (brightness) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (!Settings.System.canWrite(this)) {
//                    MaterialAlertDialogBuilder(this)
//                        .setTitle(R.string.permission_dialog_title)
//                        .setMessage(R.string.permission_dialog_desc)
//                        .setPositiveButton(resources.getString(R.string.dialog_dynamic_btn)) { dialog, _ ->
//                            dialog.cancel()
//                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
//                            intent.data = Uri.parse("package:$packageName")
//                            startActivity(intent)
//                        }
//                        .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
//                            dialog.cancel()
//                        }
//                        .show()
//                }
//            }
//        }
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val showRequestScreen = sharedPreferences.getBoolean("request",false)
        if (!showRequestScreen) {
            val editor:SharedPreferences.Editor = sharedPreferences.edit()
            editor.putBoolean("request",true)
            editor.apply()
            val i = Intent(this@HomeActivity, RequestPermissionActivity::class.java)
            startActivity(i)
        }
    }

    private fun newCardListener() {
        val database = Firebase.database.reference
        val userId = Firebase.auth.currentUser?.uid
        database.child("users").child(userId.toString()).child("cards_request").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val children = dataSnapshot.children
                var requestExist = false
                dynamicCardRequest.removeAllViews()
                fun append(arr: Array<String>, element: String): Array<String> {
                    val list: MutableList<String> = arr.toMutableList()
                    list.add(element)
                    return list.toTypedArray()
                }
                var idCardsArray = arrayOf("")
                var shopCardsArray = arrayOf("")
                var typeCardsArray = arrayOf("")
                val checkedColorsArray = booleanArrayOf(false, false, false, false, false, false, false, false, false, false, false, false)
                var eachIt = 0
                var badgeCounter = 0
                val localCard: MutableList<String> = mutableListOf<String>()
                children.forEach {
                    badgeCounter++
                    requestExist = true
                    localCard.add(it.child("shop").value.toString())
                    localCard.add(it.key.toString())
                    if (eachIt == 0) {
                        shopCardsArray[0] = it.child("shop").value.toString()
                        typeCardsArray[0] = it.child("type").value.toString()
                        idCardsArray[0] = it.key.toString()
                    } else {
                        shopCardsArray = append(shopCardsArray, it.child("shop").value.toString())
                        typeCardsArray = append(typeCardsArray, it.child("type").value.toString())
                        idCardsArray = append(idCardsArray, it.key.toString())
                    }
                    eachIt++
                }
                globalCardsList.add(localCard.toMutableList())
                if (requestExist) {
                    val badge = bottom_navigation.getOrCreateBadge(R.id.home)
                    badge.isVisible = true
                    badge.number = badgeCounter

                    val card = MaterialCardView(this@HomeActivity, null, R.attr.materialCardViewElevatedStyle)
                    card.layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
                    val param = card.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,20,50,20)
                    card.layoutParams = param

                    val button = MaterialButton(this@HomeActivity, null, androidx.appcompat.R.attr.borderlessButtonStyle)
                    button.setText(R.string.view_inv)
                    button.setOnClickListener {
                        val builder = MaterialAlertDialogBuilder(this@HomeActivity)

                        builder.setTitle(resources.getString(R.string.card_add_req_title))
                        builder.setMultiChoiceItems(shopCardsArray, checkedColorsArray) { _, which, isChecked ->
                            checkedColorsArray[which] = isChecked
                        }
                        builder.setPositiveButton(resources.getString(R.string.inv_dialog_accept)) { dialog, _ ->
                            for (i in checkedColorsArray.indices) {
                                val checked = checkedColorsArray[i]
                                if (checked) {
                                    val newCardId = idCardsArray[i]
                                    val newCardShop = shopCardsArray[i]
                                    val newCardType = typeCardsArray[i]
                                    val deleteQuery: Query = database.child("users").child(userId.toString()).child("cards_request").child(newCardId)
                                    deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            for (deleteSnapshot in dataSnapshot.children) {
                                                deleteSnapshot.ref.removeValue()
                                                database
                                                    .child("users")
                                                    .child(userId.toString())
                                                    .child("cards")
                                                    .child(newCardId).setValue(newCard(newCardId, newCardType, newCardShop))
                                                dialog.dismiss()
                                            }
                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {

                                        }
                                    })
                                }
                            }
                        }
                        builder.setNeutralButton(resources.getString(R.string.inv_dialog_cancel)) { dialog, which ->
                            dialog.dismiss()
                        }
                        val dialog = builder.create()
                        dialog.show()
                    }
                    val params2: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        240,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    params2.setMargins(750, 0, 0, 0)
                    button.layoutParams = params2

                    val descText = TextView(this@HomeActivity)
                    descText.text = resources.getString(R.string.card_add_req_toast)
                    descText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    descText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                    val params3: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params3.setMargins(40, 48, 0, 0)
                    descText.layoutParams = params3

                    card.addView(descText)
                    card.addView(button)
                    dynamicCardRequest.addView(card)
                } else {
                    dynamicCardRequest.removeAllViews()
                    val badge = bottom_navigation.getOrCreateBadge(R.id.home)
                    badge.isVisible = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun checkLinkAddCard() {
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val addShop = sharedPreferences.getString("addShop","").toString()
        val addClientId = sharedPreferences.getString("addClientId","").toString()
        val addCardType = sharedPreferences.getString("addCardType","").toString()
        if (addShop != "") {
            val editor:SharedPreferences.Editor =  sharedPreferences.edit()
            editor.putString("addShop","")
            editor.putString("addClientId","")
            editor.putString("addCardType","")
            editor.apply()
            editor.commit()

            val userId = Firebase.auth.currentUser?.uid
            val builder = MaterialAlertDialogBuilder(this@HomeActivity)
            builder.setTitle(resources.getString(R.string.link_add_card_title))
            builder.setMessage(getString(R.string.link_add_card_desc, addShop, addClientId))
            builder.setPositiveButton(resources.getString(R.string.add)) { dialog, _ ->
                database
                    .child("users")
                    .child(userId.toString())
                    .child("cards")
                    .child(addClientId).setValue(newCard(addClientId, addCardType, addShop))
                dialog.dismiss()
            }
            builder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.show()
        }
    }

    private fun checkLinkInvitations() {
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        val newFriendId = sharedPreferences.getString("friendRequest","").toString()
        if (newFriendId != "") {
            val editor:SharedPreferences.Editor =  sharedPreferences.edit()
            editor.putString("friendRequest","")
            editor.apply()
            editor.commit()
            database.child("users").child(newFriendId).get().addOnSuccessListener {
                val newFriendName = it.child("username").value.toString()
                val newFriendNotifId = it.child("notify_id").value.toString()
                if (newFriendName == null) {
                    val builder = MaterialAlertDialogBuilder(this@HomeActivity)
                    builder.setTitle(resources.getString(R.string.coming_inv_title))
                    builder.setMessage(getString(R.string.coming_inv_desc_error))
                    builder.setPositiveButton(resources.getString(R.string.dialog_dynamic_btn)) { dialog, which ->
                        dialog.dismiss()
                    }
                    val dialog = builder.create()
                    dialog.show()
                } else {
                    val myName = sharedPreferences.getString("globalUsername","").toString()
                    val myEmail = sharedPreferences.getString("globalEmail","").toString()
                    val userId = Firebase.auth.currentUser?.uid
                    val builder = MaterialAlertDialogBuilder(this@HomeActivity)
                    builder.setTitle(resources.getString(R.string.coming_inv_title))
                    builder.setMessage(getString(R.string.coming_inv_desc, newFriendName))
                    builder.setPositiveButton(resources.getString(R.string.coming_inv_send)) { dialog, _ ->
                        database
                            .child("users")
                            .child(newFriendId)
                            .child("friends_request")
                            .child(userId.toString()).setValue(newFriend(myName, myEmail))
                        OneSignal.postNotification(JSONObject("{'contents': {'en':'${getString(R.string.notification_new_friend_req)}'}, 'include_player_ids': ['$newFriendNotifId']}"),
                            object : OneSignal.PostNotificationResponseHandler {
                                override fun onSuccess(response: JSONObject) {
                                    Log.i("OneSignalExample", "postNotification Success: $response")
                                }

                                override fun onFailure(response: JSONObject) {
                                    Log.e("OneSignalExample", "postNotification Failure: $response")
                                }
                            })
                        dialog.dismiss()
                        Toast.makeText(this, R.string.coming_inv_send_msg, Toast.LENGTH_SHORT).show()
                    }
                    builder.setNegativeButton(resources.getString(R.string.inv_dialog_delete)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    val dialog = builder.create()
                    dialog.show()
                }
            }.addOnFailureListener{

            }
        }
    }

    private fun inviteListener() {
        val database = Firebase.database.reference
        val userId = Firebase.auth.currentUser?.uid
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        database.child("users").child(userId.toString()).child("friends_request").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val children = dataSnapshot.children
                var requestExist = false
                friendsRequestLayout.removeAllViews()
                fun append(arr: Array<String>, element: String): Array<String> {
                    val list: MutableList<String> = arr.toMutableList()
                    list.add(element)
                    return list.toTypedArray()
                }
                var idFriendsArray = arrayOf("")
                var nameFriendsArray = arrayOf("")
                var emailFriendsArray = arrayOf("")
                val checkedColorsArray = booleanArrayOf(false, false, false, false, false, false, false, false, false, false, false, false)
                var eachIt = 0
                var badgeCounter = 0
                children.forEach {
                    badgeCounter++
                    requestExist = true
                    if (eachIt == 0) {
                        nameFriendsArray[0] = it.child("name").value.toString()
                        emailFriendsArray[0] = it.child("email").value.toString()
                        idFriendsArray[0] = it.key.toString()
                    } else {
                        nameFriendsArray = append(nameFriendsArray, it.child("name").value.toString())
                        emailFriendsArray = append(emailFriendsArray, it.child("email").value.toString())
                        idFriendsArray = append(idFriendsArray, it.key.toString())
                    }
                    eachIt++
                }
                if (requestExist) {
                    val badge = bottom_navigation.getOrCreateBadge(R.id.friends)
                    badge.isVisible = true
                    badge.number = badgeCounter

                    val card = MaterialCardView(this@HomeActivity, null, R.attr.materialCardViewElevatedStyle)
                    card.layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
                    val param = card.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,20,50,20)
                    card.layoutParams = param

                    val button = MaterialButton(this@HomeActivity, null, androidx.appcompat.R.attr.borderlessButtonStyle)
                    button.setText(R.string.view_inv)
                    button.setOnClickListener {
                        val builder = MaterialAlertDialogBuilder(this@HomeActivity)

                        builder.setTitle(resources.getString(R.string.inv_dialog_title))
                        builder.setMultiChoiceItems(nameFriendsArray, checkedColorsArray) { _, which, isChecked ->
                            checkedColorsArray[which] = isChecked
                        }
                        builder.setPositiveButton(resources.getString(R.string.inv_dialog_accept)) { dialog, _ ->
                            for (i in checkedColorsArray.indices) {
                                val checked = checkedColorsArray[i]
                                if (checked) {
                                    val newFriendId = idFriendsArray[i]
                                    val newFriendName = nameFriendsArray[i]
                                    val newFriendEmail = emailFriendsArray[i]
                                    val myName = sharedPreferences.getString("globalUsername","").toString()
                                    val myEmail = sharedPreferences.getString("globalEmail","").toString()
                                    val deleteQuery: Query = database.child("users").child(userId.toString()).child("friends_request").child(newFriendId)
                                    deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            for (deleteSnapshot in dataSnapshot.children) {
                                                deleteSnapshot.ref.removeValue()
                                                database
                                                    .child("users")
                                                    .child(userId.toString())
                                                    .child("friends")
                                                    .child(newFriendId).setValue(newFriend(newFriendName, newFriendEmail))
                                                database
                                                    .child("users")
                                                    .child(newFriendId)
                                                    .child("friends")
                                                    .child(userId.toString()).setValue(newFriend(myName, myEmail))
                                                dialog.dismiss()
                                            }
                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {

                                        }
                                    })
                                }
                            }
                        }
                        builder.setNegativeButton(resources.getString(R.string.inv_dialog_delete)) { dialog, _ ->
                            for (i in checkedColorsArray.indices) {
                                val checked = checkedColorsArray[i]
                                if (checked) {
                                    val newFriendId = idFriendsArray[i]
                                    val deleteQuery: Query = database.child("users").child(userId.toString()).child("friends_request").child(newFriendId)
                                    deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                                            for (deleteSnapshot in dataSnapshot.children) {
                                                deleteSnapshot.ref.removeValue()
                                                dialog.dismiss()
                                            }
                                        }

                                        override fun onCancelled(databaseError: DatabaseError) {

                                        }
                                    })
                                }
                            }
                        }
                        builder.setNeutralButton(resources.getString(R.string.inv_dialog_cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        val dialog = builder.create()
                        dialog.show()
                    }
                    val params2: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        240,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    params2.setMargins(750, 0, 0, 0)
                    button.layoutParams = params2

                    val descText = TextView(this@HomeActivity)
                    descText.text = "${resources.getString(R.string.inv_title)} ($badgeCounter)"
                    descText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    descText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                    val params3: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params3.setMargins(40, 48, 0, 0)
                    descText.layoutParams = params3

                    card.addView(descText)
                    card.addView(button)
                    friendsRequestLayout.addView(card)
                } else {
                    friendsRequestLayout.removeAllViews()
                    val badge = bottom_navigation.getOrCreateBadge(R.id.friends)
                    badge.isVisible = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("jfksdhkjfhds", error.toString())
            }
        })
    }

    private var myActMode: ActionMode? = null
    private var myActMenu: Menu? = null
    var selectedItems: MutableList<View> = mutableListOf()

    private fun makeCardLayout() {
        val storage = Firebase.storage
        val userId = Firebase.auth.currentUser?.uid
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        var my_image: Bitmap
        val ref = storage.reference.child("profiles/$userId/avatar.jpg")
        try {
            val localFile: File = File.createTempFile("Images", "bmp")
            ref.getFile(localFile)
                .addOnSuccessListener(OnSuccessListener<FileDownloadTask.TaskSnapshot?> {
                    my_image = BitmapFactory.decodeFile(localFile.absolutePath)
                    val base64String: String = ImageUtil.convert(my_image)
                    val editor: SharedPreferences.Editor =  sharedPreferences.edit()
                    editor.putString("globalProfilePicBase64",base64String)
                    editor.apply()
                    my_image.circularIt(applicationContext).apply {
                        navDrawerProfilePic.setImageDrawable(this)
                        avatar_imgview.setImageDrawable(this)
                    }
                }).addOnFailureListener(
                    OnFailureListener {
                        val photoUrl = auth.currentUser?.photoUrl.toString()
                        if (photoUrl == null) {
                            navDrawerProfilePic.setImageResource(R.drawable.ic_baseline_no_accounts_24)
                            avatar_imgview.setImageResource(R.drawable.ic_baseline_no_accounts_24)
                        } else {
                            Glide.with(this)
                                .asBitmap()
                                .load(photoUrl)
                                .into(object : SimpleTarget<Bitmap?>() {
                                    override fun onResourceReady(
                                        resource: Bitmap,
                                        transition: Transition<in Bitmap?>?
                                    ) {
                                        val base64String: String = ImageUtil.convert(resource)
                                        val editor: SharedPreferences.Editor =  sharedPreferences.edit()
                                        editor.putString("globalProfilePicBase64",base64String)
                                        editor.apply()
                                        my_image = resource
                                        my_image.circularIt(applicationContext)?.apply {
                                            navDrawerProfilePic.setImageDrawable(this)
                                            avatar_imgview.setImageDrawable(this)
                                            saveProfileData(resource)
                                        }
                                    }
                                })
                        }
                    })
        } catch (e: IOException) {
            e.printStackTrace()
            val photoUrl = auth.currentUser?.photoUrl.toString()
            Glide.with(this).load(photoUrl).into(avatar_imgview)
        }

        val database = Firebase.database.reference
        dynamic.orientation = LinearLayout.VERTICAL
        database.child("users").child(userId.toString()).child("cards").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var emptyData: Boolean = true
                progressBar.visibility = View.GONE
                dynamic.removeAllViews()
                dataSnapshot.children.forEach {
                    emptyData = false
                    val shop = it.child("shop").value.toString()
                    val type = it.child("type").value.toString()
                    val clientid = it.child("clientid").value.toString()
                    val v: View = layoutInflater.inflate(R.layout.component_home_card, null)
                    val mainCard: MaterialCardView = v.findViewById<View>(R.id.main_card) as MaterialCardView
                    mainCard.setOnClickListener {
                        if (myActMode != null) {
                            if (selectedItems.contains(v)) {
                                mainCard.isChecked = false
                                val iconCard: ImageView = v.findViewById<View>(R.id.card_icon) as ImageView
                                if (type == "barcode") {
                                    changeImageCard(R.drawable.ic_barcode_fill0_wght400_grad0_opsz48, iconCard)
                                } else {
                                    changeImageCard(R.drawable.ic_qr_code_2_fill0_wght400_grad0_opsz48, iconCard)
                                }
                                selectedItems.remove(v)
                            } else {
                                val iconCard: ImageView = v.findViewById<View>(R.id.card_icon) as ImageView
                                changeImageCard(R.drawable.ic_baseline_check_circle_24, iconCard)
                                mainCard.isChecked = true
                                selectedItems.add(v)
                            }
                            if (selectedItems.isNotEmpty()) {
                                myActMode?.title = "Wybrano "+selectedItems.count()
                                if (selectedItems.count() > 1) {
                                    myActMenu?.removeItem(R.id.share)
//                                    myActMenu?.removeItem(R.id.copy)
                                } else {
                                    myActMenu?.add(0,R.id.share,1,"Share")
                                    val item: MenuItem? = myActMenu?.findItem(R.id.share)
                                    item?.setIcon(R.drawable.ic_outline_share_24)

//                                    myActMenu?.add(0,R.id.copy,2,"Copy")
//                                    val item2: MenuItem? = myActMenu?.findItem(R.id.copy)
//                                    item2?.setIcon(R.drawable.ic_baseline_content_copy_24)
                                }
                            } else {
                                myActMode?.finish()
                            }
                        } else {
                            val editor: SharedPreferences.Editor =  sharedPreferences.edit()
                            editor.putString("curr_shop",shop)
                            editor.putString("curr_type",type)
                            editor.putString("curr_clientid",clientid)
                            editor.putBoolean("private_card",false)
                            editor.apply()
                            val i = Intent(this@HomeActivity, ScanCardActivity::class.java)
                            startActivity(i)
                        }
                    }
                    mainCard.setOnLongClickListener(OnLongClickListener {
                        selectedItems.add(v)
                        val iconCard: ImageView = v.findViewById<View>(R.id.card_icon) as ImageView
                        changeImageCard(R.drawable.ic_baseline_check_circle_24, iconCard)
                        mainCard.isChecked = true
                        if (myActMode != null) {
                            return@OnLongClickListener false
                        }
                        myActMode = startSupportActionMode(myActModeCallback)
                        true
                    })
                    val titleCard: TextView = v.findViewById<View>(R.id.shop_title) as TextView
                    titleCard.text = shop.capitalize()
                    val numberCard: TextView = v.findViewById<View>(R.id.card_number) as TextView
                    numberCard.text = clientid
                    val typeCard: TextView = v.findViewById<View>(R.id.card_type) as TextView
                    typeCard.text = type
                    val iconCard: ImageView = v.findViewById<View>(R.id.card_icon) as ImageView
                    if (type == "barcode") {
                        iconCard.setImageResource(R.drawable.ic_barcode_fill0_wght400_grad0_opsz48)
                    } else {
                        iconCard.setImageResource(R.drawable.ic_qr_code_2_fill0_wght400_grad0_opsz48)
                    }
                    val buttonOpen: Button = v.findViewById<View>(R.id.open_btn) as Button
                    buttonOpen.setOnClickListener {
                        val editor: SharedPreferences.Editor =  sharedPreferences.edit()
                        editor.putString("curr_shop",shop)
                        editor.putString("curr_type",type)
                        editor.putString("curr_clientid",clientid)
                        editor.putBoolean("private_card",false)
                        editor.apply()
                        val i = Intent(this@HomeActivity, ScanCardActivity::class.java)
                        startActivity(i)
                    }
                    dynamic.addView(v)
                }
                if (emptyData) {
                    val v: View = layoutInflater.inflate(R.layout.component_archive_empty, null)
                    val iconCard: ImageView = v.findViewById<View>(R.id.scan_img) as ImageView
                    iconCard.setImageResource(R.drawable.ic_baseline_add_circle_24)
                    dynamic.addView(v)
                } else {
                    val layoutSpace = LinearLayout(this@HomeActivity, null)
                    layoutSpace.layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    val param = layoutSpace.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,20,50,40)
                    layoutSpace.layoutParams = param
                    dynamic.addView(layoutSpace)
                }
                cardReady = true
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.message)
            }
        })

        dynamic_private.orientation = LinearLayout.VERTICAL
        database.child("users").child(userId.toString()).child("cards_private").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var emptyData: Boolean = true
                progressBar.visibility = View.GONE
                dynamic_private.removeAllViews()
                dataSnapshot.children.forEach {
                    emptyData = false
                    val shop = it.child("shop").value.toString()
                    val type = it.child("type").value.toString()
                    val clientid = it.child("clientid").value.toString()
                    val v: View = layoutInflater.inflate(R.layout.component_home_card_private, null)
                    val mainCard: MaterialCardView = v.findViewById<View>(R.id.main_card) as MaterialCardView
                    mainCard.setOnClickListener {
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.putString("curr_shop",shop)
                        editor.putString("curr_type",type)
                        editor.putString("curr_clientid",clientid)
                        editor.putBoolean("private_card",true)
                        editor.apply()
                        biometricPrompt.authenticate(promptInfo)
                    }
                    val titleCard: TextView = v.findViewById<View>(R.id.shop_title) as TextView
                    titleCard.text = shop.capitalize()
                    val typeCard: TextView = v.findViewById<View>(R.id.card_type) as TextView
                    typeCard.text = type
                    val buttonOpen: Button = v.findViewById<View>(R.id.open_btn) as Button
                    buttonOpen.setOnClickListener {
                        val editor: SharedPreferences.Editor =  sharedPreferences.edit()
                        editor.putString("curr_shop",shop)
                        editor.putString("curr_type",type)
                        editor.putString("curr_clientid",clientid)
                        editor.putBoolean("private_card",true)
                        editor.apply()
                        biometricPrompt.authenticate(promptInfo)
                    }
                    if (!biometricAvailable) {
                        mainCard.isEnabled = false
                        buttonOpen.isEnabled = false
                    }
                    dynamic_private.addView(v)
                }
                if (!emptyData) {
                    val layoutSpace = LinearLayout(this@HomeActivity, null)
                    layoutSpace.layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    val param = layoutSpace.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,100,50,100)
                    layoutSpace.layoutParams = param
                    dynamic_private.addView(layoutSpace)
                }
                cardReady = true
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.message)
            }
        })
    }

    object ImageUtil {
        @Throws(IllegalArgumentException::class)
        fun convert(base64Str: String): Bitmap {
            val decodedBytes: ByteArray = Base64.decode(
                base64Str.substring(base64Str.indexOf(",") + 1),
                Base64.DEFAULT
            )
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }

        fun convert(bitmap: Bitmap): String {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        }
    }

    private fun changeImageCard(resId: Int, imgView: ImageView) {
        imgView.rotationY = 0f
        imgView.animate().rotationY(90f).setDuration(130).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                imgView.setImageResource(resId)
                imgView.rotationY = 270f
                imgView.animate().rotationY(360f).setListener(null)
            }
        })
    }

    private fun menuIconWithText(r: Drawable, title: String): CharSequence {
        r.setBounds(0, 0, r.intrinsicWidth, r.intrinsicHeight)
        val sb = SpannableString("    $title")
        val imageSpan = ImageSpan(r, ImageSpan.ALIGN_BOTTOM)
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    val myActModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.contextual_home_menu, menu)
            mode?.title = "Wybrano "+selectedItems.count()
            myActMenu = menu
//            mode?.customView = layoutInflater.inflate(R.layout.component_actionbar, null)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.archive -> {
                    MaterialAlertDialogBuilder(this@HomeActivity)
                        .setTitle("Archiwizacja")
                        .setMessage("Czy chcesz zarchiwizować następującą liczbę elementów: "+selectedItems.count())
                        .setIcon(R.drawable.ic_baseline_archive_24)
                        .setPositiveButton("Archiwizuj") { dialog, _ ->
                            dialog.cancel()
                            val userId = Firebase.auth.currentUser?.uid
                            selectedItems.forEach {
                                val titleCard: TextView = it.findViewById<View>(R.id.shop_title) as TextView
                                val numberCard: TextView = it.findViewById<View>(R.id.card_number) as TextView
                                val typeCard: TextView = it.findViewById<View>(R.id.card_type) as TextView
                                Log.d("fsdjkfjdsf", numberCard.text.toString())
                                val deleteQuery: Query = database.child("users").child(userId.toString()).child("cards").child(numberCard.text.toString())
                                deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        for (deleteSnapshot in dataSnapshot.children) {
                                            deleteSnapshot.ref.removeValue()
                                            val card = newCard(numberCard.text.toString(), typeCard.text.toString(), titleCard.text.toString())
                                            database.child("users").child(userId.toString()).child("archive").child(numberCard.text.toString()).setValue(card)
                                        }
                                    }
                                    override fun onCancelled(databaseError: DatabaseError) {}
                                })
                            }
                            mode?.finish()
                        }
                        .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                            dialog.cancel()
                        }
                        .show()
                    true
                }
                R.id.delete -> {
                    MaterialAlertDialogBuilder(this@HomeActivity)
                        .setTitle("Usuwanie")
                        .setMessage("Czy na pewno chcesz usunąć następującą liczbę elementów: "+selectedItems.count())
                        .setIcon(R.drawable.ic_baseline_delete_24)
                        .setPositiveButton("Usuń") { dialog, _ ->
                            dialog.cancel()
                            val userId = Firebase.auth.currentUser?.uid
                            var itemsToDelete: MutableList<String> = mutableListOf()
                            selectedItems.forEach {
                                val numberCard: TextView = it.findViewById<View>(R.id.card_number) as TextView
                                itemsToDelete.add(numberCard.text.toString())
                            }
                            val deleteQuery: Query = database.child("users").child(userId.toString()).child("cards")
                            deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    for (deleteSnapshot in dataSnapshot.children) {
                                        println(itemsToDelete)
                                        if (itemsToDelete.contains(deleteSnapshot.key)) {
                                            deleteSnapshot.ref.removeValue()
                                        }
                                    }
                                }
                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                            mode?.finish()
                        }
                        .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                            dialog.cancel()
                        }
                        .show()
                    true
                }
                R.id.share -> {
                    if (selectedItems.count() == 1) {
                        selectedItems.forEach {
                            val titleCard: TextView =
                                it.findViewById<View>(R.id.shop_title) as TextView
                            val numberCard: TextView =
                                it.findViewById<View>(R.id.card_number) as TextView
                            val typeCard: TextView =
                                it.findViewById<View>(R.id.card_type) as TextView
                            val sendShop = titleCard.text.toString()
                            val sendClientId = numberCard.text.toString()
                            val cardType = typeCard.text.toString()
                            var linkSendShop = sendShop.replace(" ", "+")
                            val link =
                                "https://keepcards.page.link/?link=https://keepcards-qamarq.firebaseapp.com/?type%3Dadd_card%26shop_name%3D$linkSendShop%26clientId%3D$sendClientId%26cardType%3D$cardType&apn=com.qamarq.keepcards"
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "text/plain"
                            intent.putExtra(
                                Intent.EXTRA_TEXT,
                                getString(R.string.link_add_card_send, sendShop, link)
                            )
                            startActivity(Intent.createChooser(intent, getString(R.string.share_title)))
                            mode?.finish()
                        }
                    } else {
                        Toast.makeText(this@HomeActivity, "Nie możesz mieć dwóch zaznaczonych opcji jednoczenie", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
//                R.id.copy -> {
//                    if (selectedItems.count() == 1) {
//                        selectedItems.forEach {
//                            val numberCard: TextView =
//                                it.findViewById<View>(R.id.card_number) as TextView
//                            setClipboard(this@HomeActivity, numberCard.text.toString())
//                            Toast.makeText(this@HomeActivity, R.string.success_copy, Toast.LENGTH_SHORT).show()
//                        }
//                    } else {
//                        Toast.makeText(this@HomeActivity, "Nie możesz mieć dwóch zaznaczonych opcji jednoczenie", Toast.LENGTH_SHORT).show()
//                    }
//                    true
//                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            myActMode = null
            selectedItems.forEach {
                val mainCard: MaterialCardView = it.findViewById<View>(R.id.main_card) as MaterialCardView
                val iconCard: ImageView = it.findViewById<View>(R.id.card_icon) as ImageView
                val typeCard: TextView = it.findViewById<View>(R.id.card_type) as TextView
                if (typeCard.text.toString() == "barcode") {
                    iconCard.setImageResource(R.drawable.ic_barcode_fill0_wght400_grad0_opsz48)
                } else {
                    iconCard.setImageResource(R.drawable.ic_qr_code_2_fill0_wght400_grad0_opsz48)
                }
                mainCard.isChecked = false
            }
            selectedItems.clear()
        }
    }

//    fun ImageViewAnimatedChange(c: Context?, v: ImageView, new_image: Bitmap?) {
//        val anim_out: Animation = AnimationUtils.loadAnimation(c, android.R.anim.fade_out)
//        val anim_in: Animation = AnimationUtils.loadAnimation(c, android.R.anim.fade_in)
//        anim_out.setAnimationListener(object : AnimationListener() {
//            fun onAnimationStart(animation: Animation?) {}
//            fun onAnimationRepeat(animation: Animation?) {}
//            fun onAnimationEnd(animation: Animation?) {
//                v.setImageBitmap(new_image)
//                anim_in.setAnimationListener(object : AnimationListener() {
//                    fun onAnimationStart(animation: Animation?) {}
//                    fun onAnimationRepeat(animation: Animation?) {}
//                    fun onAnimationEnd(animation: Animation?) {}
//                })
//                v.startAnimation(anim_in)
//            }
//        })
//        v.startAnimation(anim_out)
//    }

    private fun makeFriendsLayout() {
        val database = Firebase.database.reference
        val userId = Firebase.auth.currentUser?.uid
        dynamic.orientation = LinearLayout.VERTICAL
        database.child("users").child(userId.toString()).child("friends").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var emptyData: Boolean = true
                progressBarFriend.visibility = View.GONE
                friendsLayout.removeAllViews()
                var lastCard: MaterialCardView? = null
                dataSnapshot.children.forEach {
                    emptyData = false
                    val name = it.child("name").value.toString()
                    val email = it.child("email").value.toString()
                    val friendId = it.key.toString()

                    friendsLayout.orientation = LinearLayout.VERTICAL
                    val card = MaterialCardView(this@HomeActivity, null, R.attr.materialCardViewElevatedStyle)
                    lastCard = card
                    card.layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    val param = card.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,20,50,20)
                    card.layoutParams = param

                    val button2 = MaterialButton(this@HomeActivity, null, R.attr.materialButtonStyle)
                    button2.text = ""
                    button2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_delete_outline_friend_24, 0, 0, 0);
                    button2.setOnClickListener {
                        val deleteQuery: Query = database.child("users").child(userId.toString()).child("friends").child(friendId)
                        deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (deleteSnapshot in dataSnapshot.children) {
                                    deleteSnapshot.ref.removeValue()
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {}
                        })
                    }
                    val params4: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        190,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params4.setMargins(750, 80, 80, 80)
                    button2.layoutParams = params4

                    val titleText = TextView(this@HomeActivity)
                    titleText.text = name.capitalize()
                    titleText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21F)
                    val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(200, 80, 10, 10)
                    titleText.layoutParams = params

                    val descText = TextView(this@HomeActivity)
                    descText.text = email
                    descText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    descText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                    val params2: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params2.setMargins(200, 150, 10, 10)
                    descText.layoutParams = params2

                    val imgView = ImageView(this@HomeActivity)
                    val params5 = LinearLayout.LayoutParams(130, 130)
                    params5.setMargins(50, 80, 10, 10)
                    imgView.layoutParams = params5
                    val storage = Firebase.storage
                    var my_image: Bitmap?
                    val ref = storage.reference.child("profiles/$friendId/avatar.jpg")
                    try {
                        val localFile: File = File.createTempFile("Images", "bmp")
                        ref.getFile(localFile)
                            .addOnSuccessListener(OnSuccessListener<FileDownloadTask.TaskSnapshot?> {
                                my_image = BitmapFactory.decodeFile(localFile.absolutePath)
                                my_image?.circularIt(applicationContext)?.apply {
                                    imgView.setImageDrawable(this)
                                }
                            }).addOnFailureListener(
                                OnFailureListener {
                                    imgView.setImageResource(R.drawable.ic_baseline_no_accounts_24)
                                })
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    card.addView(titleText)
                    card.addView(descText)
                    card.addView(button2)
                    card.addView(imgView)
                    friendsLayout.addView(card)
                }
                if (emptyData) {
//                    val no_data_label = TextView(this@HomeActivity)
//                    no_data_label.setText(R.string.no_friends_label)
//                    no_data_label.textAlignment = View.TEXT_ALIGNMENT_CENTER
//                    no_data_label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
//                    val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.MATCH_PARENT,
//                        LinearLayout.LayoutParams.MATCH_PARENT
//                    )
//                    params.setMargins(80, 10, 80, 10)
//                    no_data_label.layoutParams = params
//                    friendsLayout.addView(no_data_label)
                    val v: View = layoutInflater.inflate(R.layout.component_archive_empty, null)
                    val iconCard: ImageView = v.findViewById<View>(R.id.scan_img) as ImageView
                    iconCard.setImageResource(R.drawable.ic_baseline_add_circle_24)
                    dynamic.addView(v)
                } else {
                    val layoutSpace = LinearLayout(this@HomeActivity, null)
                    layoutSpace.layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    val param = layoutSpace.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,100,50,100)
                    layoutSpace.layoutParams = param
                    dynamic.addView(layoutSpace)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.message)
            }
        })
    }

    fun Bitmap.circularIt(
        context: Context,
        diameter:Int = min(width,height)
    ): RoundedBitmapDrawable {
        val length = min(diameter,min(width,height))

        val bitmap = Bitmap.createBitmap(length,length,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        canvas.drawBitmap(
            this,
            (diameter - this.width)/2F,
            (diameter - this.height)/2F,
            null
        )

        return RoundedBitmapDrawableFactory.create(context.resources,bitmap).apply {
            isCircular = true
            setAntiAlias(true)
        }
    }

    private fun saveProfileData(bitmap: Bitmap) {
        val userId = Firebase.auth.currentUser?.uid
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val storage = Firebase.storage
        val ref = storage.reference.child("profiles/$userId/avatar.jpg")
        val uploadTask = ref.putBytes(data)
        uploadTask.addOnFailureListener {
        }.addOnSuccessListener {
            val parentLayout = findViewById<View>(android.R.id.content)
            Snackbar.make(parentLayout, R.string.new_pic_save, Snackbar.LENGTH_LONG)
                .setAnchorView(R.id.extended_fab)
                .setAction("Action", null).show()
            bitmap.circularIt(applicationContext).apply {
                navDrawerProfilePic.setImageDrawable(this)
                avatar_imgview.setImageDrawable(this)
            }
        }
    }

    private fun profileFragementCode() {
        avatar_imgview.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
                intent.type = "image/*"
                startActivityForResult(intent, pickImage)
            } else {
                val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                startActivityForResult(gallery, pickImage)
            }
//            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
//            startActivityForResult(gallery, pickImage)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            avatar_imgview.setImageURI(imageUri)
            val bitmap = (avatar_imgview.drawable as BitmapDrawable).bitmap
            saveProfileData(bitmap)
        } else if (requestCode == RC_APP_UPDATE) {
            if (resultCode != RESULT_OK) {

            }
        }
    }


}