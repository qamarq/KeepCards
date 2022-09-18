package com.qamarq.keepcards

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.actionCodeSettings
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ktx.database
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_login_fragment.*
import kotlinx.android.synthetic.main.activity_register_fragment.*


class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val sharedPrefFile = "keepcardspref"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        val currentUser = auth.currentUser
        if(currentUser != null){
//            val i = Intent(this@LoginActivity, HomeActivity::class.java)
//            startActivity(i)
        }

        getDynamicLinks()

        val vf = findViewById<View>(R.id.vf) as ViewFlipper
        login_menu_btm.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.login -> {
                    vf.displayedChild = 2
                    true
                }
                R.id.register -> {
                    vf.displayedChild = 1
                    true
                }
                else -> false
            }
        }

//        toggleGroup.addOnButtonCheckedListener { toggleButtonGroup, checkedId, isChecked ->
//            if (isChecked) {
//                when (checkedId) {
//                    R.id.login -> {
//                        vf.displayedChild = 2
//                    }
//                    R.id.register -> {
//                        vf.displayedChild = 1
//                    }
//                    R.id.exit -> {
//                        Toast.makeText(this, "Bye bye", Toast.LENGTH_SHORT).show()
//                        finish()
//                        exitProcess(0)
//                    }
//                }
//            } else {
//                if (toggleButtonGroup.checkedButtonId == View.NO_ID) {
//                    Toast.makeText(this, "No selected", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }

        fun verifyRegister(email: String, pass: String) {
//            auth.createUserWithEmailAndPassword(email, pass)
//                .addOnCompleteListener(this) { task ->
//                    if (task.isSuccessful) {
                        val actionCodeSettings = actionCodeSettings {
                            url = "https://keepcards.page.link/"
                            handleCodeInApp = true
                            setIOSBundleId("com.example.ios")
                            setAndroidPackageName(
                                "com.qamarq.keepcards",
                                true,
                                null)
                        }

                        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,Context.MODE_PRIVATE)
                        auth.sendSignInLinkToEmail(email, actionCodeSettings)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val editor:SharedPreferences.Editor =  sharedPreferences.edit()
                                    editor.putString("globalUsername",name_register_txt.text.toString())
                                    editor.putString("globalEmail",email)
                                    editor.putString("globalPass",pass)
                                    editor.apply()
                                    editor.commit()
                                    name_register_txt.setText("")
                                    email_register_txt.setText("")
                                    password_register_txt.setText("")
                                    password_register_txt_repeat.setText("")
                                    Toast.makeText(baseContext, R.string.email_send, Toast.LENGTH_SHORT).show()
                                    Firebase.auth.signOut()
                                    try {
                                        val intent = Intent(Intent.ACTION_MAIN)
                                        intent.addCategory(Intent.CATEGORY_APP_EMAIL)
                                        this.startActivity(intent)
                                    } catch (e: ActivityNotFoundException) {}
                                } else {
                                    login_action_btn.isEnabled = true
                                    Log.i("Login", task.exception.toString())
                                }
                            }
//                    } else {
//                        Toast.makeText(baseContext, task.exception.toString(), Toast.LENGTH_LONG).show()
//                    }
//                }
        }

        fun Activity.hideSoftKeyboard() {
            currentFocus?.let {
                val inputMethodManager = ContextCompat.getSystemService(this, InputMethodManager::class.java)!!
                inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }

        register_action_btn.setOnClickListener{
            val emailText: String = email_register_txt.text.toString()
            val passText: String = password_register_txt.text.toString()
            val repeatPass: String = password_register_txt_repeat.text.toString()
            val nameUser: String = name_register_txt.text.toString()
            val t = emailText.isNotEmpty() && passText.isNotEmpty() && repeatPass.isNotEmpty() && nameUser.isNotEmpty() && passText == repeatPass
            if (t) {
                register_action_btn.isEnabled = false
                hideSoftKeyboard()
                MaterialAlertDialogBuilder(this)
                    .setTitle(resources.getString(R.string.dialog_title_warn))
                    .setMessage(resources.getString(R.string.dialog_supporting_text_warn))
                    .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                        dialog.cancel()
                    }
                    .setPositiveButton(resources.getString(R.string.nextb)) { _, _ ->
                        verifyRegister(emailText, passText)
                    }
                    .show()
            } else {
                Toast.makeText(baseContext, R.string.valide_form, Toast.LENGTH_LONG).show()
            }
        }

        reset_pass_btn.setOnClickListener {
            startActivity(Intent(this@LoginActivity, ResetPasswordAcitvity::class.java))
        }

        login_action_btn.setOnClickListener {
            val emailText: String = email_login_txt.text.toString()
            val passText: String = password_login_txt.text.toString()

            val t = emailText.isNotEmpty() && passText.isNotEmpty()
            if (t) {
                login_action_btn.isEnabled = false
                hideSoftKeyboard()
                password_login_txt.setText("")
                auth.signInWithEmailAndPassword(emailText, passText)
                    .addOnCompleteListener(this) { task ->
                        Log.d("fdshkjfdshf", "tak: $task")
                        if (task.isSuccessful) {
                            val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,Context.MODE_PRIVATE)
                            val editor:SharedPreferences.Editor =  sharedPreferences.edit()
                            editor.putString("globalEmail",email_login_txt.text.toString())
                            editor.apply()
                            editor.commit()
                            val i = Intent(this@LoginActivity, HomeActivity::class.java)
                            startActivity(i)
                        } else {
                            login_action_btn.isEnabled = true
                            Toast.makeText(baseContext, task.exception?.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener(this) { err ->
                        Log.d("fdshkjfdshf", "nie: $err")
                    }
                    .addOnCanceledListener(this) {
                        Log.d("fdshkjfdshf", "cancel: $it")
                    }
            } else {
                Toast.makeText(baseContext, R.string.valide_form, Toast.LENGTH_LONG).show()
            }
        }
    }



    @IgnoreExtraProperties
    data class User(val username: String? = null, val email: String? = null) {
        // Null default values create a no-argument default constructor, which is needed
        // for deserialization from a DataSnapshot.
    }

    private fun getDynamicLinks() {
        Firebase.dynamicLinks
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->
                var deepLink: Uri? = null
                if (pendingDynamicLinkData != null) {
                    deepLink = pendingDynamicLinkData.link
                }

                auth = Firebase.auth
                val currentUser = auth.currentUser
                val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,Context.MODE_PRIVATE)
                if (deepLink != null) {
                    if (currentUser == null) {
                        val emailLink = deepLink.toString()
                        val globalEmail = sharedPreferences.getString("globalEmail","").toString()
                        val globalUsername = sharedPreferences.getString("globalUsername","").toString()
                        if (globalEmail == "" || globalUsername == "") {
                            Toast.makeText(this, R.string.try_again, Toast.LENGTH_SHORT).show()
                        } else {
                            val builder = MaterialAlertDialogBuilder(this@LoginActivity)
                            builder.setTitle(resources.getString(R.string.link_dialog_title))
                            builder.setMessage(resources.getString(R.string.link_dialog_desc))
                            builder.setCancelable(false)
                            val dialog = builder.show()
                            if (auth.isSignInWithEmailLink(emailLink)) {
                                auth.signInWithEmailLink(globalEmail, emailLink)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = Firebase.auth.currentUser
                                            val globalPass = sharedPreferences.getString("globalPass","").toString()
                                            val userId = user?.uid.toString()
                                            val database = Firebase.database.reference
                                            database.child("users").child(userId).setValue(User(globalUsername, globalEmail))
                                            user!!.updatePassword(globalPass)
                                                .addOnCompleteListener { status ->
                                                    if (status.isSuccessful) {
                                                        dialog.dismiss()
                                                        val i = Intent(this@LoginActivity, HomeActivity::class.java)
                                                        startActivity(i)
                                                    }
                                                }
                                        } else {
                                            dialog.dismiss()
                                            MaterialAlertDialogBuilder(this)
                                                .setTitle(resources.getString(R.string.dialog_dynamic_title))
                                                .setMessage(resources.getString(R.string.dialog_dynamic_desc))
                                                .setPositiveButton(resources.getString(R.string.dialog_dynamic_btn)) { dialog2, _ ->
                                                    dialog2.cancel()
                                                }
                                                .show()
                                        }
                                    }
                            }
                        }
                    } else {
                        //https://keepcards.page.link/?link=https://keepcards-qamarq.firebaseapp.com/?type%3Dfriend_request%26friendId%3D12345&apn=com.qamarq.keepcards
                        val req_type = deepLink.getQueryParameter("type")
                        if (req_type == "friend_request") {
                            val friendId = deepLink.getQueryParameter("friendId")
                            val editor:SharedPreferences.Editor =  sharedPreferences.edit()
                            editor.putString("friendRequest",friendId)
                            editor.apply()
                            editor.commit()
                        } else if (req_type == "add_card") {
                            val addShop = deepLink.getQueryParameter("shop_name")
                            val addClientId = deepLink.getQueryParameter("clientId")
                            val addCardType = deepLink.getQueryParameter("cardType")
                            val editor:SharedPreferences.Editor =  sharedPreferences.edit()
                            editor.putString("addShop",addShop)
                            editor.putString("addClientId",addClientId)
                            editor.putString("addCardType",addCardType)
                            editor.apply()
                            editor.commit()
                        }
                        val i = Intent(this@LoginActivity, HomeActivity::class.java)
                        startActivity(i)
                    }
                }
            }
            .addOnFailureListener(this) { e -> Log.w("LoginActivity", "getDynamicLink:onFailure", e) }
    }
}