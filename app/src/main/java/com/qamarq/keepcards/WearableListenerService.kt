package com.qamarq.keepcards

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.gms.wearable.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DataLayerListenerService : WearableListenerService() {
    var datapath = "/data_path"
    private val TAG = "PhoneActivity"
    private val sharedPrefFile = "keepcardspref"
    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sync = settingsPrefs.getBoolean("sync", true)
        for (event in dataEventBuffer) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (datapath == path) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val message = dataMapItem.dataMap.getString("message")
                    if (message != null) {
                        if ("give_me_cards" in message) {
                            if (sync) sendCardsToWatch()
                        } else if ("add_card" in message) {
                            val i = Intent(this, AddActivity::class.java)
                            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(i)
                        } else if ("check_update" in message) {
                            val i = Intent(this, AddActivity::class.java)
                            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(i)
                            try {
                                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                                storeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(storeIntent)
                            } catch (e: ActivityNotFoundException) {
                                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                                storeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(storeIntent)
                            }
                        } else if ("open_card" in message) {
                            val list = message.split("|")
                            var shopName = ""
                            var productId = ""
                            var cardType = ""
                            list.forEachIndexed { index, s ->
                                when (index) {
                                    1 -> { shopName = s }
                                    2 -> { productId = s }
                                    3 -> { cardType = s }
                                }
                            }
                            val sharedPreferences: SharedPreferences = this.getSharedPreferences(sharedPrefFile,
                                Context.MODE_PRIVATE)
                            val editor: SharedPreferences.Editor =  sharedPreferences.edit()
                            editor.putString("curr_shop",shopName)
                            editor.putString("curr_type",cardType)
                            editor.putString("curr_clientid",productId)
                            editor.apply()
                            val i = Intent(this, ScanCardActivity::class.java)
                            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(i)
                        } else if ("delete_card" in message) {
                            val list = message.split("|")
                            var productId = ""
                            list.forEachIndexed { index, s ->
                                when (index) {
                                    1 -> { productId = s }
                                }
                            }
                            val userId = Firebase.auth.currentUser?.uid
                            val database = Firebase.database.reference
                            val deleteQuery: Query = database.child("users").child(userId.toString()).child("cards").child(productId)
                            deleteQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    for (deleteSnapshot in dataSnapshot.children) {
                                        deleteSnapshot.ref.removeValue()
                                        sendCardsToWatch()
                                        val notifying = settingsPrefs.getBoolean("notify_delete", false)
                                        if (notifying) Toast.makeText(this@DataLayerListenerService, R.string.delete_notify, Toast.LENGTH_SHORT).show()
//                                        Toast.makeText(this, R.string.sync_notify, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                        } else if ("check_connection" in message) {
                            val time = System.currentTimeMillis()
                            sendData("connection_success|$time")
                        }
                    }
                } else {
                    Log.e(TAG, "Unrecognized path: $path")
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                Log.v(TAG, "Data deleted : " + event.dataItem.toString())
            } else {
                Log.e(TAG, "Unknown data event Type = " + event.type)
            }
        }
    }

    private fun sendCardsToWatch() {
        val database = Firebase.database.reference
        val userId = Firebase.auth.currentUser?.uid
        val myList : MutableList<String> = mutableListOf()
        database.child("users").child(userId.toString()).child("cards").get().addOnSuccessListener { it ->
            it.children.forEach {
                val time = System.currentTimeMillis()
                val shop = it.child("shop").value.toString()
                val clientid = it.child("clientid").value.toString()
                val type = it.child("type").value.toString()
                myList.add("$shop|$clientid|$type|$time")
            }
            val time = System.currentTimeMillis()
            var myBigString = "$time"
            var firstTime = true
            myList.forEach {
                if (firstTime) {
                    myBigString = it
                    firstTime = false
                } else {
                    myBigString = "$myBigString#$it"
                }
            }
            sendData(myBigString)
            val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            val notifying = settingsPrefs.getBoolean("notify_sync", false)
            if (notifying) Toast.makeText(this, R.string.sync_notify, Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendData(message: String) {
        val dataMap = PutDataMapRequest.create(datapath)
        dataMap.dataMap.putString("message", message)
        val request = dataMap.asPutDataRequest()
        request.setUrgent()
        val dataItemTask = Wearable.getDataClient(this).putDataItem(request)
        dataItemTask
            .addOnSuccessListener { dataItem ->
                Log.d(
                    TAG,
                    "Sending message was successful: $dataItem"
                )
            }
            .addOnFailureListener { e -> Log.e(TAG, "Sending message failed: $e") }
    }
}