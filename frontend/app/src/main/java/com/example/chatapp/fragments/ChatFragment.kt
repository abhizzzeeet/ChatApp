package com.example.chatapp.fragments

import android.content.Intent
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
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.chatapp.R
import com.example.chatapp.models.Chat
import com.example.chatapp.models.Message
import com.example.chatapp.models.Participants
import com.example.chatapp.models.PreviousChat
import com.example.chatapp.recyclerView.MessageAdapter
import com.example.chatapp.viewModels.SharedDataRepository
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject

class ChatFragment(
    private val name: String,
    private val phoneNumber: String,
    private var receiverId: String?,
    private var chatId: String?,
    private val position: Int,
    private var callback: OnBackListener

) : Fragment(R.layout.fragment_chat) {

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
    private lateinit var statusReference: DatabaseReference
    var userExists: Boolean = false
    private val messagesList = mutableListOf<Message>()
    private var senderId: String? = null
    private var lastMessage: String? = null
    private var timestamp: Long =0L
    var flag=0


    interface OnBackListener {
        fun onLastMessageUpdate(position: Int,lastMessage: String?,newChat: PreviousChat)
    }

    //    private var receiverId: String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        // Assume chatId is known or passed as argument
        messagesReference = FirebaseDatabase.getInstance().getReference("messages")
        chatsReference = FirebaseDatabase.getInstance().getReference("chats")
        statusReference = FirebaseDatabase.getInstance().getReference("status")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        println("$name , $phoneNumber ")

        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        nameTextView = view.findViewById(R.id.nameTextView)
        messageInputContainer = view.findViewById(R.id.message_input_container)
        sendButton = view.findViewById(R.id.send_button)
        messageEditText = view.findViewById(R.id.message_edit_text)
        messagesRecyclerView = view.findViewById(R.id.message_recycler_view)


        nameTextView.text = name


        SharedDataRepository.getMessage().observe(viewLifecycleOwner, Observer { message ->
            Log.d("ChatFragment2", "Message: $message")
            senderId = message
            Log.d("ChatFragment2", "UserId: $senderId")
            if(senderId !=null){
                ChatFragmentOperations()
            }
        })





        sendButton.setOnClickListener {
            if(messageEditText.text!=null){
                sendMessage(chatId)
            }

        }

        view.findViewById<ImageView>(R.id.backButton).setOnClickListener {
            performBackOperations()
        }

        // Handle system back button press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                performBackOperations()
            }
        })


        return view
    }
    private fun ChatFragmentOperations(){

        messagesRecyclerView.layoutManager = LinearLayoutManager(context)
        messageAdapter = MessageAdapter(messagesList,senderId)
        messagesRecyclerView.adapter = messageAdapter

        if (chatId != null) {
            // ChatId is provided (existing chat)
            messagesReference = messagesReference.child(chatId.toString())
            chatsReference = chatsReference.child(chatId.toString())
            // Fetch existing messages first
            messagesReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        if (message != null) {
                            messagesList.add(message)
                            lastMessage = message.text
                        }
                    }
                    Log.d("MessageFetched ChatFragment","$messagesList")
                    messageAdapter.notifyDataSetChanged()
                    messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatFragment", "Failed to load messages: ${error.message}")
                }
            })
//            listenForNewMessages()
        } else {
            // ChatId is not provided (new chat)
            // Assuming you want to create a new chatId and store messages
            val newChatRef = messagesReference.push() // Generate new chatId
            val newChatId = newChatRef.key // Get the generated chatId
            flag=1
            if (newChatId != null) {
                chatId = newChatId.toString()
                messagesReference =
                    newChatRef // Reference messages under new chatId
                chatsReference = chatsReference.child(newChatId)
//                listenForNewMessages()
            } else {
                Log.d("ERROR ChatFragment ", "ERROR in creating chatId")
            }
        }
        listenForNewMessages()
    }

    private fun sendMessage(chatId: String?) {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty() && senderId != null) {
            val messageId = messagesReference.push().key
            val timeStamp = System.currentTimeMillis()
            timestamp = timeStamp
            val message = Message("$senderId", messageText, timeStamp)
            val chat = Chat(Participants("$senderId", "$receiverId"), messageText, timeStamp)
            if (messageId != null) {
                messagesReference.child(messageId).setValue(message)
                chatsReference.setValue(chat)
                messageEditText.text.clear()
                Log.d("Sended Message","$messageText")
//                messagesList.add(message)
//                lastMessage=message.text
//                Log.d("Received Message","${message.text}")
//                messageAdapter.notifyItemInserted(messagesList.size - 1)
//                messagesRecyclerView.scrollToPosition(messagesList.size - 1)
//                listenForNewMessages()

                // Send FCM notification

                sendNotification(receiverId, messageText)

            }
        }
    }

    private fun listenForNewMessages() {
        Log.d("Listenfor newMessage","ListenForNewMessage")
        messagesReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    messagesList.add(message)
                    lastMessage = message.text
                    messageAdapter.notifyItemInserted(messagesList.size - 1)
                    messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatFragment", "Failed to listen for new messages: ${error.message}")
            }
        })
    }

    private fun performBackOperations() {
        val newChat= PreviousChat(chatId.toString(),receiverId.toString(),name,phoneNumber,lastMessage,timestamp)
        if(flag==1 && lastMessage!=null){
            callback.onLastMessageUpdate(-1, lastMessage, newChat)
            // Simulate back press to pop the fragment and return to ChatActivity
            parentFragmentManager.popBackStack()
        }
        else{
            callback.onLastMessageUpdate(position, lastMessage,newChat)
            // Simulate back press to pop the fragment and return to ChatActivity
            parentFragmentManager.popBackStack()
        }

    }

    private fun sendNotification(receiverId: String?, messageText: String) {
        if (receiverId == null || senderId == null){
            Log.d("SenderId or ReceiverId is null","SenderId or ReceiverId is null")
            return
        }
        Log.d("ChatFragment","Message Text: $messageText , ReceiverId: $receiverId")
        statusReference.child(receiverId.toString()).child(senderId.toString()).child("chatOpen")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatOpen = snapshot.getValue(Boolean::class.java) ?: false
                    if (!chatOpen) {
                        // Send notification only if the chat fragment is not open
                        val url = "https://us-central1-chatapp-cbe0b.cloudfunctions.net/sendNotification"
                        val jsonObject = JSONObject()
                        jsonObject.put("receiverId", receiverId)
                        jsonObject.put("messageText", messageText)
                        Log.d("ChatFragment","Before backend call Message Text: $messageText , ReceiverId: $receiverId")
                        val request = JsonObjectRequest(
                            Request.Method.POST, url, jsonObject,
                            { response ->
                                Log.d("FCM", "Notification sent successfully: $response")
                            },
                            { error ->
                                Log.e("FCM", "Error sending notification: $error")
                            }
                        )

                        Volley.newRequestQueue(context).add(request)
                    } else {
                        Log.d("FCM", "Chat fragment is open, notification not sent.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FCM", "Failed to check chat status: ${error.message}")
                }
            })
    }

    override fun onResume() {
        super.onResume()
        // Set status to open when the fragment is resumed
        setStatus(true)
    }

    override fun onPause() {
        super.onPause()
        // Set status to closed when the fragment is paused
        setStatus(false)
    }

    private fun setStatus(isOpen: Boolean) {
        val statusMap = mapOf("chatOpen" to isOpen)
        statusReference.child(senderId.toString()).child(receiverId.toString()).updateChildren(statusMap)
    }
}