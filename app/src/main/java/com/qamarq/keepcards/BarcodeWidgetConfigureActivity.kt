package com.qamarq.keepcards

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.qamarq.keepcards.databinding.BarcodeWidgetConfigureBinding
import kotlinx.android.synthetic.main.barcode_widget_configure.*

/**
 * The configuration screen for the [BarcodeWidget] AppWidget.
 */
class BarcodeWidgetConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var appWidgetText: EditText
    private val sharedPrefFile = "keepcardspref"
    private var onClickListener = View.OnClickListener {
        val context = this@BarcodeWidgetConfigureActivity

        // When the button is clicked, store the string locally
        val widgetText = appWidgetText.text.toString()
        saveTitlePref(context, appWidgetId, widgetText)

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, appWidgetId)

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: BarcodeWidgetConfigureBinding

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = BarcodeWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val storage = Firebase.storage
        val userId = Firebase.auth.currentUser?.uid

        val database = Firebase.database.reference
        cardList.orientation = LinearLayout.VERTICAL
        val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
            Context.MODE_PRIVATE)
        database.child("users").child(userId.toString()).child("cards").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var emptyData: Boolean = true
                cardList.removeAllViews()
                var lastCard: MaterialCardView? = null
                dataSnapshot.children.forEach {
                    emptyData = false
                    val shop = it.child("shop").getValue().toString()
                    val type = it.child("type").getValue().toString()
                    val clientid = it.child("clientid").getValue().toString()

                    val card = MaterialCardView(this@BarcodeWidgetConfigureActivity, null, R.attr.materialCardViewElevatedStyle)
                    card.layoutParams =
                        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    val param = card.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,20,50,20)
                    card.layoutParams = param
                    lastCard = card
                    val button = MaterialButton(this@BarcodeWidgetConfigureActivity, null, R.attr.materialButtonStyle)
                    button.setText(R.string.add_widget)
                    if (type == "barcode") {
                        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_barcode_fill0_wght400_grad0_opsz24, 0, 0, 0)
                    } else {
                        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_qr_code_2_fill0_wght400_grad0_opsz24, 0, 0, 0)
                    }
                    button.setOnClickListener {
                        val context = this@BarcodeWidgetConfigureActivity
                        // When the button is clicked, store the string locally
                        saveTitlePref(context, appWidgetId, clientid)
                        saveShopPref(context, appWidgetId, shop)
                        saveTypePref(context, appWidgetId, type)

                        // It is the responsibility of the configuration activity to update the app widget
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        updateAppWidget(context, appWidgetManager, appWidgetId)

                        // Make sure we pass back the original appWidgetId
                        val resultValue = Intent()
                        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                    val params3: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params3.setMargins(80, 330, 80, 80)
                    button.layoutParams = params3

                    val titleText = TextView(this@BarcodeWidgetConfigureActivity)
                    titleText.text = shop.capitalize()
                    titleText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24F)
                    val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(80, 80, 10, 10)
                    titleText.layoutParams = params

                    val descText = TextView(this@BarcodeWidgetConfigureActivity)
                    descText.text = clientid
                    descText.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    descText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                    val params2: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params2.setMargins(80, 180, 10, 10)
                    descText.layoutParams = params2

                    card.addView(titleText)
                    card.addView(descText)
                    card.addView(button)
                    cardList.addView(card)
                }
                if (emptyData) {
                    val no_data_label = TextView(this@BarcodeWidgetConfigureActivity)
                    no_data_label.setText(R.string.no_data_label)
                    no_data_label.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    no_data_label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
                    val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(80, 10, 80, 10)
                    no_data_label.layoutParams = params
                    cardList.addView(no_data_label)
                } else {
                    val param = lastCard?.layoutParams as ViewGroup.MarginLayoutParams
                    param.setMargins(50,20,50,450)
                    lastCard?.layoutParams ?: params
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", error.message) //Never ignore potential errors!
            }
        })
    }

}

private const val PREFS_NAME = "com.qamarq.keepcards.BarcodeWidget"
private const val PREF_PREFIX_KEY = "appwidget_"
private const val PREF_PREFIX_KEY_SHOP = "appwidget_shop_"
private const val PREF_PREFIX_KEY_TYPE = "appwidget_type_"

// Write the prefix to the SharedPreferences object for this widget
internal fun saveTitlePref(context: Context, appWidgetId: Int, text: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.putString(PREF_PREFIX_KEY + appWidgetId, text)
    prefs.apply()
}

internal fun saveShopPref(context: Context, appWidgetId: Int, text: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.putString(PREF_PREFIX_KEY_SHOP + appWidgetId, text)
    prefs.apply()
}

internal fun saveTypePref(context: Context, appWidgetId: Int, text: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.putString(PREF_PREFIX_KEY_TYPE + appWidgetId, text)
    prefs.apply()
}

// Read the prefix from the SharedPreferences object for this widget.
// If there is no preference saved, get the default from a resource
internal fun loadTitlePref(context: Context, appWidgetId: Int): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    val titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
    return titleValue ?: context.getString(R.string.appwidget_text)
}

internal fun loadShopPref(context: Context, appWidgetId: Int): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    val titleValue = prefs.getString(PREF_PREFIX_KEY_SHOP + appWidgetId, null)
    return titleValue ?: context.getString(R.string.appwidget_text)
}

internal fun loadTypePref(context: Context, appWidgetId: Int): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    val titleValue = prefs.getString(PREF_PREFIX_KEY_TYPE + appWidgetId, null)
    return titleValue ?: context.getString(R.string.appwidget_text)
}

internal fun deleteTitlePref(context: Context, appWidgetId: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.remove(PREF_PREFIX_KEY + appWidgetId)
    prefs.apply()
}