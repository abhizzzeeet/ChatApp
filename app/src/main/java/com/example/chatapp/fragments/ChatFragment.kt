package com.example.chatapp.fragments

import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.recyclerView.MessageAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatFragment(private val name: String , private val  phoneNumber: String) : Fragment() {

    private lateinit var nameTextView: TextView
    private lateinit var messageInputContainer: LinearLayout
    private lateinit var inviteButton: Button
    private lateinit var databaseReference: DatabaseReference
    private lateinit var messageEditText: EditText
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messagesReference: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private val messagesList = mutableListOf<Message>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        println("$name , $phoneNumber ")
        val view =inflater.inflate(R.layout.fragment_chat, container, false)

        nameTextView = view.findViewById(R.id.nameTextView)
        messageInputContainer = view.findViewById(R.id.message_input_container)
        inviteButton = view.findViewById(R.id.invite_button)
        nameTextView.text = name

        checkPhoneNumberInDatabase(phoneNumber)
        return view
    }
    private fun stripPhoneNumber(phoneNumber: String): Pair<String, String> {
        val cleanedNumber = phoneNumber.replace(" ", "").replace("-", "")
        val lastTenDigits = if (cleanedNumber.length >= 10) cleanedNumber.takeLast(10) else cleanedNumber
        val countryCode = if (cleanedNumber.length > 10) cleanedNumber.dropLast(10) else ""
        return Pair(countryCode, lastTenDigits)
    }

    private fun checkPhoneNumberInDatabase(phoneNumber: String) {
        val (fragCountryCode, fragLastTenDigits) = stripPhoneNumber(phoneNumber)
        Log.d("contryCode Extracted in fragment","$fragCountryCode")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userExists = false
                for (data in snapshot.children) {
                    val dbPhoneNumber = data.child("phone").value.toString()
                    val (dbCountryCode, dbLastTenDigits) = stripPhoneNumber(dbPhoneNumber)
                    Log.d("phone numbers extracted from db", "$dbLastTenDigits")

                    if (dbLastTenDigits == fragLastTenDigits) {
                        userExists = true
                        Log.d("Found user","Found user with number $dbLastTenDigits")
                        break
                    }
                }

                if (userExists) {
                    messageInputContainer.visibility = View.VISIBLE
                    inviteButton.visibility = View.GONE
                } else {
                    messageInputContainer.visibility = View.GONE
                    inviteButton.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
                Log.e("ERROR in checking user presence", "$error")

            }
        })
    }
}