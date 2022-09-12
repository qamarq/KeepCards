package com.qamarq.keepcards

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_search_friend.*


class SearchFriendFragment : DialogFragment() {


    private var toolbar: Toolbar? = null
    private val sharedPrefFile = "keepcardspref"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window!!.setLayout(width, height)

//            search_email.editText?.doOnTextChanged { inputText, _, _, _ ->
//                Log.d("text", inputText.toString())
//            }

            inv1.setOnClickListener {
                val userId = Firebase.auth.currentUser?.uid
                val link =
                    "https://keepcards.page.link/?link=https://keepcards-qamarq.firebaseapp.com/?type%3Dfriend_request%26friendId%3D$userId&apn=com.qamarq.keepcards"
                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message, link))
                startActivity(Intent.createChooser(intent, "Share Link"))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view: View = inflater.inflate(R.layout.fragment_search_friend, container, false)
        toolbar = view.findViewById(R.id.toolbar)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar!!.setNavigationOnClickListener { dismiss() }
        toolbar!!.setTitle(R.string.add_friend)
        toolbar!!.inflateMenu(R.menu.fullscreen_dialog)
        toolbar!!.setOnMenuItemClickListener {
            dismiss()
            true
        }
    }

    companion object {
        const val TAG = "example_dialog"
        fun display(fragmentManager: FragmentManager?): SearchFriendFragment {
            val SearchFriendFragment = SearchFriendFragment()
            SearchFriendFragment.show(fragmentManager!!, TAG)
            return SearchFriendFragment
        }
    }
}