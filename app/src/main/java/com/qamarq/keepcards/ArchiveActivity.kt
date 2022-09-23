package com.qamarq.keepcards

import android.content.Intent
import android.content.res.Resources.Theme
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
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


class ArchiveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archive)

        val userId = Firebase.auth.currentUser?.uid
        val database = Firebase.database.reference
        dynamicArchive.orientation = LinearLayout.VERTICAL
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
                var lastCard: MaterialCardView? = null
                dataSnapshot.children.forEach {
                    emptyData = false
                    val shop = it.child("shop").value.toString()
                    val type = it.child("type").value.toString()
                    val clientid = it.child("clientid").value.toString()

                    val card = MaterialCardView(this@ArchiveActivity, null, R.attr.materialCardViewElevatedStyle)
                    card.layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    val param = card.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,20,50,20)
                    card.layoutParams = param
                    lastCard = card


                    val linearLayout = LinearLayout(this@ArchiveActivity, null)
                    linearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    linearLayout.orientation = LinearLayout.VERTICAL

                    val linearLayout2 = LinearLayout(this@ArchiveActivity, null)
                    linearLayout2.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    linearLayout2.orientation = LinearLayout.VERTICAL
                    linearLayout2.setPadding(16,16,16,16)


                    val linearLayout3 = LinearLayout(this@ArchiveActivity, null)
                    linearLayout3.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    linearLayout3.orientation = LinearLayout.HORIZONTAL
                    linearLayout3.gravity = Gravity.CENTER_VERTICAL


                    val icon = ImageView(this@ArchiveActivity, null)
                    val firstDp = getPixelsFromDP(30)
                    val param2 = card.layoutParams as ViewGroup.MarginLayoutParams
                    param2.setMargins(0,0,10,0)
                    card.layoutParams = param2
                    icon.layoutParams.height = 30
                    icon.layoutParams.width = 30
                    val typedValue = TypedValue()
                    val theme: Theme = this@ArchiveActivity.theme
                    theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
                    @ColorInt val color = typedValue.data
                    icon.setColorFilter(ContextCompat.getColor(this@ArchiveActivity, color), android.graphics.PorterDuff.Mode.SRC_IN)
                    if (type == "barcode") {
                        icon.setImageResource(R.drawable.ic_barcode_fill0_wght400_grad0_opsz48)
                    } else {
                        icon.setImageResource(R.drawable.ic_qr_code_2_fill0_wght400_grad0_opsz48)
                    }

                    val cardTitle = TextView(this@ArchiveActivity)
                    cardTitle.text = shop.capitalize()
//                    cardTitle.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
//                    cardTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24F)
                    val params3: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    cardTitle.layoutParams = params3
                    theme.resolveAttribute(com.google.android.material.R.attr.textAppearanceTitleMedium, typedValue, true)
                    cardTitle.setTextAppearance(typedValue.data)

                    linearLayout3.addView(icon)
                    linearLayout3.addView(cardTitle)
                    linearLayout2.addView(linearLayout3)

                    val cardNumber = TextView(this@ArchiveActivity)
                    cardNumber.text = clientid
                    val params4: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params4.setMargins(0, 8, 0, 0)
                    cardNumber.layoutParams = params4
                    theme.resolveAttribute(com.google.android.material.R.attr.textAppearanceBodyMedium, typedValue, true)
                    cardNumber.setTextAppearance(typedValue.data)

                    val cardHelpText = TextView(this@ArchiveActivity)
                    cardHelpText.text = clientid
                    val params5: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params5.setMargins(0, 16, 0, 0)
                    cardHelpText.layoutParams = params5
                    theme.resolveAttribute(com.google.android.material.R.attr.textAppearanceBodyMedium, typedValue, true)
                    cardHelpText.setTextAppearance(typedValue.data)

                    linearLayout2.addView(cardNumber)
                    linearLayout2.addView(cardHelpText)

                    linearLayout.addView(linearLayout2)

                    val linearLayout4 = LinearLayout(this@ArchiveActivity, null)
                    linearLayout4.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    linearLayout4.orientation = LinearLayout.HORIZONTAL
                    val param6 = card.layoutParams as ViewGroup.MarginLayoutParams
                    param6.setMargins(8,8,8,8)
                    linearLayout4.layoutParams = param6

                    val button = MaterialButton(this@ArchiveActivity, null, com.google.android.material.R.style.Widget_Material3_Button_ElevatedButton_Icon)
                    button.setText("Usuń")
                    button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_delete_24, 0, 0, 0)
                    button.setOnClickListener {
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
                    val params7: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params7.weight = 1F
                    params7.setMargins(0, 0, 0, 8)
                    button.layoutParams = params7

                    val button2 = MaterialButton(this@ArchiveActivity, null, com.google.android.material.R.style.Widget_Material3_Button_Icon)
                    button2.setText("Przywróć")
                    button2.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_restore_24, 0, 0, 0)
                    button2.setOnClickListener {
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
                    val params8: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params8.weight = 1F
                    button2.layoutParams = params8

                    linearLayout4.addView(button)
                    linearLayout4.addView(button2)
                    linearLayout.addView(linearLayout4)
                    card.addView(linearLayout)


//
//                    val titleText = TextView(this@HomeActivity)
//                    titleText.text = shop.capitalize()
//                    titleText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
//                    titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24F)
//                    val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                    )
//                    params.setMargins(80, 80, 10, 10)
//                    titleText.layoutParams = params
//
//                    val descText = TextView(this@HomeActivity)
//                    descText.text = clientid
//                    descText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
//                    descText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
//                    val params2: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                    )
//                    params2.setMargins(80, 180, 10, 10)
//                    descText.layoutParams = params2

                    dynamicArchive.addView(card)
                }
                if (emptyData) {
                    val no_data_label = TextView(this@ArchiveActivity)
                    no_data_label.setText("Brak zarchiwizowanych kart")
                    no_data_label.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    no_data_label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
                    val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(80, 10, 80, 10)
                    no_data_label.layoutParams = params
                    dynamicArchive.addView(no_data_label)
                } else {
//                    val param = lastCard?.layoutParams as ViewGroup.MarginLayoutParams
//                    param.setMargins(50,20,50,450)
//                    lastCard?.layoutParams = params
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