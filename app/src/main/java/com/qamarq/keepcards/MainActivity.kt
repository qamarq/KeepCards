package com.qamarq.keepcards

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.qamarq.keepcards.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_login_fragment.*
import kotlinx.android.synthetic.main.activity_register_fragment.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    lateinit var button : Button
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val sharedPrefFile = "keepcardspref"

    companion object {
        const val RC_SIGN_IN = 1001
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 12345) {
//            if (resultCode == RESULT_OK) {
//                // Successfully signed in
//                val user = FirebaseAuth.getInstance().currentUser
//                Toast.makeText(applicationContext, "Successfully signed in!", Toast.LENGTH_SHORT)
//                    .show()
//            } else {
//                // Sign in failed. If response is null the user canceled the sign-in flow using the back button. Otherwise check
//                // response.getError().getErrorCode() and handle the error.
//                // ...
//                Toast.makeText(applicationContext, "Sign in FAILED", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        val currentUser = auth.currentUser
        if(currentUser != null){
            val i = Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(i)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        login_google.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAnchorView(R.id.login_google)
//                .setAction("Action", null).show()
            signIn()
        }

        val loginEmail = findViewById<Button>(R.id.login_email)
        loginEmail.setOnClickListener {
            val i = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(i)
        }
    }

    private fun signIn() {
        login_google.isEnabled = false
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Error: ${e}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credentials = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credentials)
            .addOnCompleteListener(this) { task ->
                login_google.isEnabled = true
                if (task.isSuccessful) {
                    val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
                        Context.MODE_PRIVATE)
                    val editor: SharedPreferences.Editor =  sharedPreferences.edit()
                    val user = Firebase.auth.currentUser
                    val userId = user?.uid.toString()
                    val userName = user?.displayName
                    val userEmail = user?.email
                    editor.putString("globalEmail",userEmail)
                    editor.putString("globalUsername",userName)
                    editor.apply()
                    editor.commit()
                    val database = Firebase.database.reference
                    database.child("users").child(userId).child("username").setValue(userName)
                    database.child("users").child(userId).child("email").setValue(userEmail)
                    val i = Intent(this@MainActivity, HomeActivity::class.java)
                    startActivity(i)
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}