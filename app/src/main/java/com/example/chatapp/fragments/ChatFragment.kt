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
import com.example.chatapp.R
import com.example.chatapp.activities.PaymentActivity
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

class ChatFragment(
    private val name: String,
    private val phoneNumber: String,
    private var receiverId: String?,
    private var chatId: String?,
    private val position: Int,
    private var callback: OnBackListener

) : Fragment() {

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
    private var senderId: String? = null
    private var lastMessage: String? = null



    interface OnBackListener {
        fun onLastMessageUpdate(position: Int,lastMessage: String?)
    }

    //    private var receiverId: String?=null

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
            sendMessage(chatId)
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
        view.findViewById<Button>(R.id.chatPayButton).setOnClickListener{
            val intent = Intent(requireContext(),PaymentActivity::class.java)
            startActivity(intent)
        }

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
        } else {
            // ChatId is not provided (new chat)
            // Assuming you want to create a new chatId and store messages
            val newChatRef = messagesReference.push() // Generate new chatId
            val newChatId = newChatRef.key // Get the generated chatId
            if (newChatId != null) {
                chatId = newChatId.toString()
                messagesReference =
                    newChatRef // Reference messages under new chatId
                chatsReference = chatsReference.child(newChatId)
            } else {
                Log.d("ERROR ChatFragment ", "ERROR in creating chatId")
            }
        }
    }

    private fun sendMessage(chatId: String?) {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty() && senderId != null) {
            val messageId = messagesReference.push().key
            val timeStamp = System.currentTimeMillis()
            val message = Message("$senderId", messageText, timeStamp)
            val chat = Chat(Participants("$senderId", "$receiverId"), messageText, timeStamp)
            if (messageId != null) {
                messagesReference.child(messageId).setValue(message)
                chatsReference.setValue(chat)
                messageEditText.text.clear()
                Log.d("Sended Message","$messageText")
                messagesList.add(message)
                lastMessage=message.text
                Log.d("Received Message","${message.text}")
                messageAdapter.notifyItemInserted(messagesList.size - 1)
                messagesRecyclerView.scrollToPosition(messagesList.size - 1)

            }
        }
    }

    private fun performBackOperations() {
        // Perform your operations here before navigating back
        // For example, save state or perform cleanup
        callback.onLastMessageUpdate(position, lastMessage)
        // Simulate back press to pop the fragment and return to ChatActivity
        parentFragmentManager.popBackStack()
    }
}