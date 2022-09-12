package com.qamarq.keepcards

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_reset_password_acitvity.*

class ResetPasswordAcitvity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password_acitvity)

        resetPassBar.setNavigationOnClickListener {
            startActivity(Intent(this@ResetPasswordAcitvity, LoginActivity::class.java))
        }

        send_code_btn.setOnClickListener {
            send_code_btn.isEnabled = false
            val emailAddress = email_reset_pass_txt.text.toString()
            if (emailAddress != "") {
                Firebase.auth.sendPasswordResetEmail(emailAddress)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val parentLayout = findViewById<View>(android.R.id.content)
                            Snackbar.make(parentLayout, "Wysłano email umożliwiający zresetowanie hasła. Sprawdź poczte", Snackbar.LENGTH_LONG).show()
                            startActivity(Intent(this@ResetPasswordAcitvity, LoginActivity::class.java))
                        }
                    }
            } else {
                val parentLayout = findViewById<View>(android.R.id.content)
                Snackbar.make(parentLayout, "Uzupełnij swój email", Snackbar.LENGTH_LONG).show()
            }
            send_code_btn.isEnabled = true
        }
    }
}