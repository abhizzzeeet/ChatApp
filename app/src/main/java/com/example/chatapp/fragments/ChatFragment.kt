package com.example.chatapp.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.models.Chat
import com.example.chatapp.models.Message
import com.example.chatapp.models.Participants
import com.example.chatapp.recyclerView.MessageAdapter
import com.example.chatapp.viewModels.SharedDataRepository
import com.google.firebase.database.ChildEventListener
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
    private lateinit var sendButton: ImageView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messagesReference: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatsReference: DatabaseReference
    var userExists: Boolean = false
    private val messagesList = mutableListOf<Message>()
    private var senderId: String?=null
    private var receiverId: String?=null
    var flag=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        // Assume chatId is known or passed as argument
        messagesReference = FirebaseDatabase.getInstance().getReference("messages")
        chatsReference = FirebaseDatabase.getInstance().getReference("chats")

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
        sendButton = view.findViewById(R.id.send_button)
        messageEditText = view.findViewById(R.id.message_edit_text)
        messagesRecyclerView = view.findViewById(R.id.message_recycler_view)


        nameTextView.text = name
        messagesRecyclerView.layoutManager = LinearLayoutManager(context)
        messageAdapter = MessageAdapter(messagesList)
        messagesRecyclerView.adapter = messageAdapter


        SharedDataRepository.getMessage().observe(viewLifecycleOwner, Observer { message ->
            Log.d("ChatFragment2", "Message: $message")
            senderId = message
            Log.d("ChatFragment2", "UserId: $senderId")
        })

        Log.d("ChatFragment", "User ID: $senderId")



        checkPhoneNumberInDatabase(phoneNumber){userExists ->
            if(userExists){
                // Check if chatId is provided (for existing chats) or create new chatId
                var chatId = arguments?.getString("chatId")
//        var chatId: String?=null

                if (chatId != null) {
                    // ChatId is provided (existing chat)
                    messagesReference = messagesReference.child(chatId)
                }
                else {
                    // ChatId is not provided (new chat)
                    // Assuming you want to create a new chatId and store messages
                    val newChatRef = messagesReference.push() // Generate new chatId
                    val newChatId = newChatRef.key // Get the generated chatId
                    if (newChatId != null) {
                        flag=1
                        chatId=newChatId.toString()
                        messagesReference =
                            newChatRef // Reference messages under new chatId
                        chatsReference = chatsReference.child(newChatId)
                    }
                    else{
                        Log.d("ERROR ChatFragment ", "ERROR in creating chatId")
                    }
                }
                sendButton.setOnClickListener {
                    sendMessage(chatId)
                }
                listenForMessages()

            }
        }

        return view
    }

    private fun sendMessage(chatId:String?) {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty() && senderId!=null) {
            val messageId = messagesReference.push().key
            val timeStamp = System.currentTimeMillis()
            val message = Message("$senderId", messageText, timeStamp)
            val chat= Chat(Participants("$senderId","$receiverId"),messageText,timeStamp)
            if (messageId != null) {
                messagesReference.child(messageId).setValue(message)
                chatsReference.setValue(chat)
                messageEditText.text.clear()

            }
        }
    }

    private fun listenForMessages() {
        messagesReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    messagesList.add(message)
                    messageAdapter.notifyItemInserted(messagesList.size - 1)
                    messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }



    private fun stripPhoneNumber(phoneNumber: String): Pair<String, String> {
        val cleanedNumber = phoneNumber.replace(" ", "").replace("-", "")
        val lastTenDigits = if (cleanedNumber.length >= 10) cleanedNumber.takeLast(10) else cleanedNumber
        val countryCode = if (cleanedNumber.length > 10) cleanedNumber.dropLast(10) else ""
        return Pair(countryCode, lastTenDigits)
    }

    private fun checkPhoneNumberInDatabase(phoneNumber: String,callback: (Boolean) -> Unit) {
        val (fragCountryCode, fragLastTenDigits) = stripPhoneNumber(phoneNumber)
        Log.d("contryCode Extracted in fragment","$fragCountryCode")

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userExists = false
                for (data in snapshot.children) {
                    val dbPhoneNumber = data.child("phone").value.toString()
                    val (dbCountryCode, dbLastTenDigits) = stripPhoneNumber(dbPhoneNumber)
                    Log.d("phone numbers extracted from db", "$dbLastTenDigits")

                    if (dbLastTenDigits == fragLastTenDigits) {
                        userExists = true
                        receiverId = data.key
                        Log.d("ReceiverId ", "$receiverId")
                        Log.d("Found user","Found user with number $dbLastTenDigits")
                        break
                    }
                    else{
                        Log.d("Chat Fragment","User Doesnt exist")
                    }
                }

                if (userExists) {
                    messageInputContainer.visibility = View.VISIBLE
                    inviteButton.visibility = View.GONE
                } else {
                    messageInputContainer.visibility = View.GONE
                    inviteButton.visibility = View.VISIBLE
                }
                callback(userExists)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
                Log.e("ERROR in checking user presence", "$error")
                callback(false)

            }
        })
    }
}