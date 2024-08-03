package com.example.chatapp.activities

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.MainActivity
import com.example.chatapp.R
import com.example.chatapp.contacts.Contact
import com.example.chatapp.contacts.ContactsAdapter
import com.example.chatapp.contacts.OnItemClickListener
import com.example.chatapp.fragments.ChatFragment
import com.example.chatapp.models.PreviousChat
import com.example.chatapp.models.User
import com.example.chatapp.recyclerView.ChatsAdapter
import com.example.chatapp.recyclerView.InviteToAppAdapter
import com.example.chatapp.recyclerView.OtherContactsAdapter
import com.example.chatapp.viewModels.SharedDataRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChatActivity : AppCompatActivity(), OnItemClickListener,ChatFragment.OnBackListener {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var btnLogout: Button

    private lateinit var recyclerViewChats: RecyclerView
    private lateinit var recyclerViewOtherContacts: RecyclerView
    private lateinit var recyclerViewInvite: RecyclerView


    private lateinit var searchContacts: EditText
    private lateinit var chatsTextView: TextView
    private lateinit var otherContactsTextView: TextView
    private lateinit var inviteTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var chatsAdapter: ChatsAdapter
    private lateinit var inviteToAppAdapter: InviteToAppAdapter
    private lateinit var otherContactsAdapter: OtherContactsAdapter
    private val contactsList = mutableListOf<Contact>()
    private val filteredPreviousChatsList = mutableListOf<PreviousChat>()
    private val filteredUsersList = mutableListOf<User>()
    private val filteredContactList = mutableListOf<Contact>()
    private val recentChatsList = mutableListOf<Contact>()
    private lateinit var databaseReference: DatabaseReference
    private lateinit var chatsReference: DatabaseReference
    private var senderId: String? = null


    private lateinit var listPreparation: ListPreparation
    private lateinit var listOperations: ListOperations

//    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView

    private lateinit var previousChatsList: MutableList<PreviousChat>
    private lateinit var usersList: MutableList<User>
    private lateinit var contactList: MutableList<Contact>

    private lateinit var loadingBottomSheet: BottomSheetDialog

    companion object {
        private const val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        mAuth = FirebaseAuth.getInstance()

        searchContacts = findViewById(R.id.searchContacts)
        chatsTextView = findViewById(R.id.chats_textview)
        otherContactsTextView = findViewById(R.id.other_contacts_textview)
        inviteTextView = findViewById(R.id.invite_textview)
        recyclerViewChats = findViewById(R.id.recyclerViewChats)
        recyclerViewOtherContacts = findViewById(R.id.recyclerViewOtherContacts)
        recyclerViewInvite = findViewById(R.id.recyclerViewInvite)
        progressBar = findViewById(R.id.progressBar)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "ChatApp"

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize BottomSheetDialog
        loadingBottomSheet = BottomSheetDialog(this)
        val bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_loading, null)
        loadingBottomSheet.setContentView(bottomSheetView)
        loadingBottomSheet.setCancelable(false)
        loadingBottomSheet.show()

        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)




        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        chatsReference = FirebaseDatabase.getInstance().getReference("chats")



//        SharedDataRepository.getMessage().observe(this, Observer { message ->
//            senderId = message
//
//        })
        CoroutineScope(Dispatchers.Main).launch {
            val senderId = getSenderId(this@ChatActivity)
            Log.d("SenderId", "$senderId")

            if(senderId!=null){
                FirebaseMessaging.getInstance().subscribeToTopic(senderId)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("FCM", "Subscribed to topic $senderId")
                        }
                    }
            }



            listPreparation = ListPreparation(senderId,this@ChatActivity,lifecycleScope)
            val (previousChats, users, contacts) = listPreparation.prepareLists()

            // Initialize the lateinit variables
            previousChatsList = previousChats
            usersList = users
            contactList = contacts

            // Sort the list based on the timestamp
            previousChatsList.sortByDescending { it.lastMessageTimestamp }
            Log.d("PreviousChatSList","$previousChatsList")

            // Step 2: Update loading progress
//            updateLoadingProgress(50)
//
//            // Step 3: Perform operations in parallel
            listOperations = ListOperations(previousChatsList, usersList, contactList)
            listOperations.performOperations()

            filteredPreviousChatsList.addAll(previousChatsList)
            chatsTextView.visibility = if (filteredPreviousChatsList.isEmpty()) View.GONE else View.VISIBLE



            recyclerViewChats.layoutManager = LinearLayoutManager(this@ChatActivity)
            chatsAdapter = ChatsAdapter(filteredPreviousChatsList, this@ChatActivity)
            recyclerViewChats.adapter = chatsAdapter

            recyclerViewOtherContacts.layoutManager = LinearLayoutManager(this@ChatActivity)
            otherContactsAdapter = OtherContactsAdapter(filteredUsersList, this@ChatActivity)
            recyclerViewOtherContacts.adapter = otherContactsAdapter

            recyclerViewInvite.layoutManager = LinearLayoutManager(this@ChatActivity)
            inviteToAppAdapter = InviteToAppAdapter(filteredContactList, this@ChatActivity)
            recyclerViewInvite.adapter = inviteToAppAdapter

//
//            // Step 4: Update loading progress
//            updateLoadingProgress(100)
//

//
//            // Hide loading UI when tasks complete
//            progressBar.visibility = View.GONE
//            loadingText.visibility = View.GONE
            loadingBottomSheet.dismiss()
        }


        searchContacts.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_chat_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                // Launch SettingsActivity or SettingsFragment
                true
            }

            R.id.menu_logout -> {
                // Launch AboutActivity or AboutFragment
                signOutAndStartSignInActivity()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }





    private fun filterContacts(query: String) {
        if (query.isEmpty()) {
            filteredPreviousChatsList.clear()
            filteredPreviousChatsList.addAll(previousChatsList)
            filteredUsersList.clear()
            filteredContactList.clear()
        } else {
            val filteredChat = previousChatsList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phoneNumber.replace("\\s".toRegex(), "").contains(query.replace("\\s".toRegex(), ""), ignoreCase = true)
            }
            val filteredUser = usersList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phoneNumber.replace("\\s".toRegex(), "").contains(query.replace("\\s".toRegex(), ""), ignoreCase = true)
            }
            val filteredContact = contactList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.phoneNumber.replace("\\s".toRegex(), "").contains(query.replace("\\s".toRegex(), ""), ignoreCase = true)
            }

            filteredPreviousChatsList.clear()
            filteredPreviousChatsList.addAll(filteredChat)

            filteredUsersList.clear()
            filteredUsersList.addAll(filteredUser)

            filteredContactList.clear()
            filteredContactList.addAll(filteredContact)

        }
        chatsTextView.visibility = if (filteredPreviousChatsList.isEmpty()) View.GONE else View.VISIBLE
        otherContactsTextView.visibility = if (filteredUsersList.isEmpty()) View.GONE else View.VISIBLE
        inviteTextView.visibility = if (filteredContactList.isEmpty()) View.GONE else View.VISIBLE
        chatsAdapter.notifyDataSetChanged()
        otherContactsAdapter.notifyDataSetChanged()
        inviteToAppAdapter.notifyDataSetChanged()
    }


    override fun onPreviousChatItemClick(previousChat: PreviousChat,position: Int) {
            val chatFragment = ChatFragment(previousChat.name, previousChat.phoneNumber, previousChat.receiverId,previousChat.chatId,position,this@ChatActivity)
            supportFragmentManager.beginTransaction()
                .replace(R.id.chatActivityContainer, chatFragment)
                .addToBackStack(null)
                .commit()

    }
    override fun onOtherContactItemClick(user: User) {
            val chatFragment = ChatFragment(user.name, user.phoneNumber, user.userId, null, 0,this@ChatActivity)
            supportFragmentManager.beginTransaction()
                .replace(R.id.chatActivityContainer, chatFragment)
                .addToBackStack(null)
                .commit()

    }

    override fun onInviteItemClick(contact: Contact) {
//        if (contact.isUser) {
//            val chatFragment = ChatFragment(contact.name, contact.phoneNumber, contact.userId)
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.chatActivityContainer, chatFragment)
//                .addToBackStack(null)
//                .commit()
//        }
    }

    private fun signOutAndStartSignInActivity() {

        mAuth.signOut()

        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            val intent = Intent(this@ChatActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    suspend fun getSenderId(lifecycleOwner: LifecycleOwner): String = suspendCancellableCoroutine { continuation ->
        SharedDataRepository.getMessage().observe(lifecycleOwner, object : Observer<String?> {
            override fun onChanged(senderId: String?) {
                if (senderId != null) {
                    continuation.resume(senderId)
                    SharedDataRepository.getMessage().removeObserver(this)
                } else {
                    continuation.resumeWithException(IllegalArgumentException("SenderId is null"))
                }
            }
        })
    }

    override fun onLastMessageUpdate(position: Int, lastMessage: String?, newChat: PreviousChat) {
        if(position ==-1 && lastMessage!=null){
            previousChatsList.add(newChat)
            Log.d("ChatActivity","newdata added")
            chatsAdapter.notifyDataSetChanged()
        }
        else{
            val viewHolder = recyclerViewChats.findViewHolderForAdapterPosition(position) as? ChatsAdapter.ChatViewHolder
            viewHolder?.itemView?.findViewById<TextView>(R.id.chatLastMessage)?.text = lastMessage
            Log.d("ChatActivity","$previousChatsList")
        }

    }

}
