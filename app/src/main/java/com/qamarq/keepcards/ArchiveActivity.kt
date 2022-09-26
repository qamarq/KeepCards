package com.qamarq.keepcards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_archive.*
import kotlinx.android.synthetic.main.fragment_home_cards.*
import org.w3c.dom.Text


class ArchiveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive)

        val userId = Firebase.auth.currentUser?.uid
        val database = Firebase.database.reference

        archiveAppBar.setNavigationOnClickListener {
            val i = Intent(this@ArchiveActivity, HomeActivity::class.java)
            startActivity(i)
        }
        database.child("users").child(userId.toString()).child("archive").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var emptyData: Boolean = true
                progressBarArchive.visibility = View.GONE
                dynamicArchive.removeAllViews()
                dataSnapshot.children.forEach {
                    emptyData = false
                    val shop = it.child("shop").value.toString()
                    val type = it.child("type").value.toString()
                    val clientid = it.child("clientid").value.toString()
                    Log.d("fdshkfhdsjf", shop)

                    val v: View = layoutInflater.inflate(R.layout.component_archive_card, null)
//                    val listview: ListView = v.findViewById<View>(R.id.streamListView) as ListView
//                    val progress = v.findViewById<View>(R.id.streamProgressBar) as ProgressBar
                    val titleCard: TextView = v.findViewById<View>(R.id.shop_title) as TextView
                    titleCard.text = shop.capitalize()
                    val numberCard: TextView = v.findViewById<View>(R.id.card_number) as TextView
                    numberCard.text = clientid
                    val buttonRestore: Button = v.findViewById<View>(R.id.restore_btn) as Button
                    buttonRestore.setOnClickListener {
                        val userId = Firebase.auth.currentUser?.uid
                        val database = Firebase.database.reference
                        val deleteQuery: Query = database.child("users").child(userId.toString()).child("archive").child(clientid)
                        deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (deleteSnapshot in dataSnapshot.children) {
                                    deleteSnapshot.ref.removeValue()
                                    val card = HomeActivity.newCard(clientid, type, shop)
                                    database.child("users").child(userId.toString()).child("cards").child(clientid).setValue(card)
                                    Toast.makeText(this@ArchiveActivity, "Przywrócono kartę", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onCancelled(databaseError: DatabaseError) {}
                        })
                    }
                    val buttonDelete: Button = v.findViewById<View>(R.id.delete_btn) as Button
                    buttonDelete.setOnClickListener {
                        val userId = Firebase.auth.currentUser?.uid
                        val database = Firebase.database.reference
                        val deleteQuery: Query = database.child("users").child(userId.toString()).child("archive").child(clientid)
                        deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (deleteSnapshot in dataSnapshot.children) {
                                    deleteSnapshot.ref.removeValue()
                                    Toast.makeText(this@ArchiveActivity, "Usunięto kartę", Toast.LENGTH_SHORT).show()
                                }
                            }
                            override fun onCancelled(databaseError: DatabaseError) {}
                        })
                    }
                    dynamicArchive.addView(v)


                }
                if (emptyData) {
//                    val no_data_label = TextView(this@ArchiveActivity)
//                    no_data_label.setText("Brak zarchiwizowanych kart")
//                    no_data_label.textAlignment = View.TEXT_ALIGNMENT_CENTER
//                    no_data_label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
//                    val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.MATCH_PARENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                    )
//                    params.setMargins(80, 10, 80, 10)
//                    no_data_label.layoutParams = params
//                    dynamicArchive.addView(no_data_label)
                    val v: View = layoutInflater.inflate(R.layout.component_archive_empty, null)
                    dynamicArchive.addView(v)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.message)
            }
        })
    }

    private fun getPixelsFromDP(i: Int): Float {
        val scale = resources.displayMetrics.density
        return (i * scale + 0.5f)
    }
}