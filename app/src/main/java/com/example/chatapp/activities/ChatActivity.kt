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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.MainActivity
import com.example.chatapp.R
import com.example.chatapp.contacts.Contact
import com.example.chatapp.contacts.ContactsAdapter
import com.example.chatapp.contacts.OnItemClickListener
import com.example.chatapp.fragments.ChatFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChatActivity : AppCompatActivity() , OnItemClickListener{

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var btnLogout: Button
//    private lateinit var btnLoadContacts: Button
    private lateinit var recyclerViewContacts: RecyclerView
    private lateinit var searchContacts: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var contactsAdapter: ContactsAdapter
    private val contactsList = mutableListOf<Contact>()
    private val filteredContactsList = mutableListOf<Contact>()


    companion object {
        private const val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

//        btnLoadContacts = findViewById(R.id.loadContacts)
        searchContacts = findViewById(R.id.searchContacts)
        recyclerViewContacts = findViewById(R.id.recyclerViewChatActivity)
        progressBar = findViewById(R.id.progressBar)

        recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        contactsAdapter = ContactsAdapter(filteredContactsList,this)
        recyclerViewContacts.adapter = contactsAdapter



        Toast.makeText(this,"Loading Contacts",Toast.LENGTH_SHORT).show()
            loadContacts()




        searchContacts.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

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
    }

    private fun signOutAndStartSignInActivity() {
        mAuth.signOut()

        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            val intent = Intent(this@ChatActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadContacts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
        } else {
            progressBar.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.Main).launch {
                val contacts = withContext(Dispatchers.IO) { getContacts() }
                contactsList.clear()
                contactsList.addAll(contacts)
                progressBar.visibility = View.GONE
                Toast.makeText(this@ChatActivity,"Contacts Loaded",Toast.LENGTH_SHORT).show()
                Log.d("Contacts List", "$contactsList")
            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                // Permission denied, handle accordingly (e.g., show a message)
            }
        }
    }

    private fun filterContacts(query: String) {
        if (query.isEmpty()) {
            filteredContactsList.clear()
        } else {
            val filteredContacts = contactsList.filter {
                it.name.contains(query, ignoreCase = true) || it.phoneNumber.replace("\\s".toRegex(), "").contains(query.replace("\\s".toRegex(), ""), ignoreCase = true)
            }
            filteredContactsList.clear()
            filteredContactsList.addAll(filteredContacts)
        }
        contactsAdapter.notifyDataSetChanged()
    }

    private fun getContacts(): List<Contact> {
        val contacts = HashSet<Contact>()
        val resolver: ContentResolver = contentResolver
        val cursor = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )

        cursor?.use { cursor ->
            val idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneNumberColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            while (cursor.moveToNext()) {
                val id = cursor.getString(idColumnIndex)
                val name = cursor.getString(nameColumnIndex)
                val hasPhoneNumber = cursor.getString(hasPhoneNumberColumnIndex)?.toInt() ?: 0

                if (hasPhoneNumber > 0) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                        arrayOf(id),
                        null
                    )

                    phoneCursor?.use { phoneCursor ->
                        val phoneNumberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                        // Check if phoneNumberIndex is valid (-1 means column not found)
                        if (phoneNumberIndex == -1) {
                            Log.e("Contacts", "Phone number column not found in phoneCursor")
                            // Handle the error case as needed
                            return@use
                        }

                        while (phoneCursor.moveToNext()) {
                            val phoneNumber = phoneCursor.getString(phoneNumberIndex)
                            contacts.add(Contact(name, phoneNumber))
                        }
                    }

                    phoneCursor?.close()
                }
            }
        }

        cursor?.close()
        return contacts.toList()
    }

    override fun onItemClick(contact: Contact) {
        val chatFragment = ChatFragment(contact.name, contact.phoneNumber)
        supportFragmentManager.beginTransaction()
            .replace(R.id.chatActivityContainer, chatFragment)
            .addToBackStack(null)
            .commit()
    }

}
