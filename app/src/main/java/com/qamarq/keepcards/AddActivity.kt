package com.qamarq.keepcards

import android.R.attr.password
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.qamarq.keepcards.LoginActivity.User
import kotlinx.android.synthetic.main.activity_add.*
import kotlinx.android.synthetic.main.fragment_home_account.*
import kotlinx.android.synthetic.main.header_navigation_drawer.*
import java.util.concurrent.Executor


class AddActivity : AppCompatActivity() {
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val sharedPrefFile = "keepcardspref"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        var selectedType = "barcode"
        val database = Firebase.database.reference
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)

        topAppBar.setNavigationOnClickListener {
            val i = Intent(this@AddActivity, HomeActivity::class.java)
            startActivity(i)
        }

//        cancelBtn.setOnClickListener {
//            val i = Intent(this@AddActivity, HomeActivity::class.java)
//            startActivity(i)
//        }

        checkDeviceHasBiometric()
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    val contextView = findViewById<View>(R.id.main_add_layout)
                    if (errString == resources.getString(R.string.cancel)) {
                        Snackbar.make(contextView, "Anulowano", Snackbar.LENGTH_SHORT)
                            .setAnchorView(scan_fab)
                            .show()
                    } else {
                        Snackbar.make(contextView, "$errString", Snackbar.LENGTH_SHORT)
                            .setAnchorView(scan_fab)
                            .show()
                    }
                    private_switch.isChecked = false
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    super.onAuthenticationSucceeded(result)
                    private_switch.isChecked = true
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
//                    val contextView = findViewById<View>(R.id.main_add_layout)
//                    Snackbar.make(contextView, "Authentication failed", Snackbar.LENGTH_SHORT)
//                        .setAnchorView(scan_fab)
//                        .show()
//                    private_switch.isChecked = false
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Potwierdź swoją tożsamość")
            .setSubtitle("Aby aktywować funkcję Bezpieczna Karta")
            .setNegativeButtonText(resources.getString(R.string.cancel))
            .build()

        // Prompt appears when user clicks "Log in".
        // Consider integrating with the keystore to unlock cryptographic operations,
        // if needed by your app.

        acceptBtn.setOnClickListener {
            data class newCard(val clientid: String? = null, val type: String? = null, val shop: String? = null) {}
            val user = Firebase.auth.currentUser
            val shop = shop_name_txt.text.toString()
            val clientid = client_id_txt.text.toString()
            val userId = user?.uid
            val card = newCard(clientid, selectedType, shop)
            if (private_switch.isChecked) {
                database.child("users").child(userId.toString()).child("cards_private").child(clientid).setValue(card)
            } else {
                database.child("users").child(userId.toString()).child("cards").child(clientid).setValue(card)
            }
            val i = Intent(this@AddActivity, HomeActivity::class.java)
            startActivity(i)
        }

        private_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                MaterialAlertDialogBuilder(this@AddActivity)
                    .setTitle("Bezpieczna karta")
                    .setMessage("Czy chcesz włączyć opcję prywatnej karty? Wszystkie dane będą przechowywane odzielnie od pozostałych kard oraz zabezpieczone biometrycznie na każdym zsynchronizowanym urządzeniu.")
                    .setIcon(R.drawable.ic_outline_lock_24)
                    .setPositiveButton("Włącz") { dialog, _ ->
                        dialog.cancel()
                        biometricPrompt.authenticate(promptInfo)
                    }
                    .setNegativeButton(resources.getString(R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                        private_switch.isChecked = false
                    }
                    .show()
            }
        }

        val barcodeLauncher = registerForActivityResult(
            ScanContract()
        ) { result: ScanIntentResult ->
            if (result.contents == null) {
//                Toast.makeText(this@AddActivity, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                client_id_txt.setText(result.contents)
                shop_name_txt.requestFocus()
            }
        }

        toggleButtonSelectType.addOnButtonCheckedListener { toggleButtonGroup, checkedId, isChecked ->
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
                if (toggleButtonGroup.checkedButtonId == View.NO_ID) {
//                    Toast.makeText(this, "No selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

        scan_fab.setOnClickListener {
            val options = ScanOptions()
            if (selectedType == "barcode") {
                options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES)
                options.setPrompt("Scan a barcode")
            } else {
                options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                options.setPrompt("Scan a QR Code")
            }
            options.setCameraId(0) // Use a specific camera of the device
            options.setBeepEnabled(false)
            options.setBarcodeImageEnabled(true)
            barcodeLauncher.launch(options)
        }
    }

    private fun checkDeviceHasBiometric() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
//                Log.d("MY_APP_TAG", "App can authenticate using biometrics.")
                private_layout.visibility = View.VISIBLE

            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
//                Log.e("MY_APP_TAG", "No biometric features available on this device.")
                private_layout.visibility = View.GONE

            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
//                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.")
                private_layout.visibility = View.GONE

            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Prompts the user to create credentials that your app accepts.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                    }
                    startActivityForResult(enrollIntent, 100)
                }

                private_switch.isClickable = false
            }
        }
    }
}