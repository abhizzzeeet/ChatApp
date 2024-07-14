package com.example.chatapp.activities

import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.example.chatapp.viewModels.SharedDataRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ChatActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var btnLogout: Button

    private lateinit var recyclerViewContacts: RecyclerView
    private lateinit var searchContacts: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var contactsAdapter: ContactsAdapter
    private val contactsList = mutableListOf<Contact>()
    private val filteredContactsList = mutableListOf<Contact>()
    private val recentChatsList = mutableListOf<Contact>()
    private lateinit var databaseReference: DatabaseReference
    private lateinit var chatsReference: DatabaseReference
    private var senderId: String? = null


    private lateinit var listPreparation: ListPreparation
    private lateinit var listOperations: ListOperations

//    private lateinit var progressBar: ProgressBar
    private lateinit var loadingText: TextView
    companion object {
        private const val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        searchContacts = findViewById(R.id.searchContacts)
        recyclerViewContacts = findViewById(R.id.recyclerViewChatActivity)
        progressBar = findViewById(R.id.progressBar)

        progressBar = findViewById(R.id.progressBar)
        loadingText = findViewById(R.id.loadingText)

        // Show loading UI
        progressBar.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE

        recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        contactsAdapter = ContactsAdapter(filteredContactsList, this)
        recyclerViewContacts.adapter = contactsAdapter
        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        chatsReference = FirebaseDatabase.getInstance().getReference("chats")


        mAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        btnLogout = findViewById(R.id.logoutButton)
        btnLogout.setOnClickListener {
            signOutAndStartSignInActivity()
        }

//        SharedDataRepository.getMessage().observe(this, Observer { message ->
//            senderId = message
//
//        })
        CoroutineScope(Dispatchers.Main).launch {
            val senderId = getSenderId(this@ChatActivity)
            Log.d("SenderId", "$senderId")

            listPreparation = ListPreparation(senderId,this@ChatActivity,lifecycleScope)
            var (previousChatsList, usersList, contactList) = listPreparation.prepareLists()
            Log.d("PreviousChatSList","$previousChatsList")

            // Step 2: Update loading progress
//            updateLoadingProgress(50)
//
//            // Step 3: Perform operations in parallel
            listOperations = ListOperations(previousChatsList, usersList, contactList)
            listOperations.performOperations()
//
//            // Step 4: Update loading progress
//            updateLoadingProgress(100)
//
//            // Now you can use previousChatList, usersList, and contactList as needed
//            // Example: Update UI with filtered contactList
//            // contactsAdapter.updateList(contactList)
//
//            // Hide loading UI when tasks complete
//            progressBar.visibility = View.GONE
//            loadingText.visibility = View.GONE
        }


//        searchContacts.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                filterContacts(s.toString())
//            }
//
//            override fun afterTextChanged(s: Editable?) {}
//        })

    }






//    private fun filterContacts(query: String) {
//        if (query.isEmpty()) {
//            filteredContactsList.clear()
//            filteredContactsList.addAll(recentChatsList)
//        } else {
//            val filteredContacts = contactsList.filter {
//                it.name.contains(query, ignoreCase = true) ||
//                        it.phoneNumber.replace("\\s".toRegex(), "").contains(query.replace("\\s".toRegex(), ""), ignoreCase = true)
//            }
//            filteredContactsList.clear()
//            filteredContactsList.addAll(filteredContacts)
//        }
//        contactsAdapter.notifyDataSetChanged()
//    }



    override fun onItemClick(contact: Contact) {
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

}
