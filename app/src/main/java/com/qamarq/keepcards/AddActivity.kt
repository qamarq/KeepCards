package com.qamarq.keepcards

import android.R.attr.password
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.qamarq.keepcards.LoginActivity.User
import kotlinx.android.synthetic.main.activity_add.*


class AddActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)
        var selectedType = "barcode"
        val database = Firebase.database.reference

        topAppBar.setNavigationOnClickListener {
            val i = Intent(this@AddActivity, HomeActivity::class.java)
            startActivity(i)
        }

        cancelBtn.setOnClickListener {
            val i = Intent(this@AddActivity, HomeActivity::class.java)
            startActivity(i)
        }

        acceptBtn.setOnClickListener {
            data class newCard(val clientid: String? = null, val type: String? = null, val shop: String? = null) {}
            val user = Firebase.auth.currentUser
            val shop = shop_name_txt.text.toString()
            val clientid = client_id_txt.text.toString()
            val userId = user?.uid
            val card = newCard(clientid, selectedType, shop)
            database.child("users").child(userId.toString()).child("cards").child(clientid).setValue(card)
            val i = Intent(this@AddActivity, HomeActivity::class.java)
            startActivity(i)
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
}