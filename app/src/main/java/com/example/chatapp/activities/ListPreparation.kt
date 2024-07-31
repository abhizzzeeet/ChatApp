package com.example.chatapp.activities

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.chatapp.contacts.Contact
import com.example.chatapp.models.PreviousChat
import com.example.chatapp.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ListPreparation(private val senderId: String?, private val context: Context, private val lifecycleScope: LifecycleCoroutineScope) {

    private var contactsList = mutableListOf<Contact>()
    private var usersList = mutableListOf<User>()
    private val PERMISSIONS_REQUEST_READ_CONTACTS = 100
    private lateinit var usersReference: DatabaseReference
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    private var contactsContinuation: Continuation<MutableList<Contact>>? = null
    private var previousChatsMap = mutableMapOf<String, PreviousChat>()

    suspend fun prepareLists(): Triple<MutableList<PreviousChat>, MutableList<User>, MutableList<Contact>> =
        coroutineScope {
            val previousChatListDeferred =
                async(Dispatchers.IO) { preparePreviousChatList(senderId) }
            val usersListDeferred = async(Dispatchers.IO) { prepareUsersList() }
            val contactListDeferred = async(Dispatchers.IO) { prepareContactList() }

            var previousChatList = previousChatListDeferred.await()
            var usersList = usersListDeferred.await()
            var contactList = contactListDeferred.await()

            previousChatList = previousChatList.toSet().toMutableList()
            Log.d("prepareList PreviousChatList","$previousChatList")
            Triple(
                previousChatList,
                usersList,
                contactList
            )

        }

    private suspend fun preparePreviousChatList(senderId: String?): MutableList<PreviousChat> {
        val previousChatsList = mutableListOf<PreviousChat>()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val chatsReference = databaseReference.child("chats")
            val chatsListDeferred = scope.async {
                chatsReference.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (chatSnapshot in snapshot.children) {
                            var chatId = chatSnapshot.key.toString()
                            val participants = chatSnapshot.child("participants").children
                            val lastMessage =
                                chatSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                            val timestamp =
                                chatSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                            Log.d("Last Message", "$lastMessage")
                            Log.d("Sender Id", "$senderId")
                            var flag = false
                            var ct = 0
                            val otherParticipants = mutableListOf<String>()
                            for (participant in participants) {
                                val id = participant.value
                                Log.d("Participant Id", "$id")
                                if (id == senderId && ct == 0) {
                                    flag = true
                                    ct += 1
                                } else {
                                    otherParticipants.add(id.toString())
                                }
                            }
                            // Check if senderId is one of the participants
                            if (flag) {
                                Log.d("True", "True")
                                for (userId in otherParticipants) {
                                    fetchUserDetails(userId) { name, phoneNumber ->
                                        if (!previousChatsMap.containsKey(chatId)) {
                                            previousChatsMap[chatId] = PreviousChat(chatId, userId, name, phoneNumber,timestamp)
                                            previousChatsList.add(PreviousChat(chatId , userId ,name, phoneNumber,timestamp))
                                            Log.d("PreviousChatsListJustAfter", "$previousChatsList")
                                            Log.d("PreviousChatsMap", "$previousChatsMap")
                                        }
                                        previousChatsList.add(PreviousChat(chatId , userId ,name, phoneNumber,timestamp))
                                        Log.d("PreviousChatsListJustAfter", "$previousChatsList")
                                    }
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.d("ERROR prepare previous chats list", "$error")
                    }
                })
            }
            Log.d("PreviousChatsList", "$previousChatsList")
            chatsListDeferred.await()
            Log.d("PreviousChatsListDeferred", "$previousChatsList")
        }
        Log.d("Returned PreviousChatsList", "$previousChatsList")
        return previousChatsList
    }

    private suspend fun prepareUsersList(): MutableList<User> {
        return suspendCoroutine { continuation ->
            usersReference = FirebaseDatabase.getInstance().getReference("users")
            usersReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        val id = data.key
                        val name = data.child("name").getValue(String::class.java).toString()
                        val phoneNumber = data.child("phone").getValue(String::class.java).toString()
                        usersList.add(User(id.toString(), name, phoneNumber))
                    }
                    continuation.resume(usersList)
                    Log.d("UsersList", "$usersList")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ERROR prepare users list", "$error")
                    continuation.resume(mutableListOf())
                }
            })
        }
    }

    private suspend fun prepareContactList(): MutableList<Contact> {
        return suspendCoroutine { continuation ->
            contactsContinuation = continuation
            loadContacts(continuation)
            Log.d("ContactsList", "$contactsList")
        }
    }

    fun fetchUserDetails(userId: String, callback: (String, String) -> Unit) {
        val usersReference = databaseReference.child("users")
        usersReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val name = userSnapshot.child("name").getValue(String::class.java).orEmpty()
                val phoneNumber = userSnapshot.child("phone").getValue(String::class.java).orEmpty()
                Log.d("Name", "$name")
                Log.d("PhoneNumber", "$phoneNumber")
                callback(name, phoneNumber)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("ERROR Fetch User Details", "$error")
            }
        })
    }

    private fun loadContacts(continuation: Continuation<MutableList<Contact>>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
        } else {
            lifecycleScope.launch {
                val contacts = withContext(Dispatchers.IO) { getContacts() }
                contactsList.clear()
                contactsList.addAll(contacts)
                Toast.makeText(context, "Contacts Loaded", Toast.LENGTH_SHORT).show()
                Log.d("Contacts List", "$contactsList")
                contactsContinuation?.resume(contactsList)
            }
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lifecycleScope.launch {
                    val contacts = withContext(Dispatchers.IO) { getContacts() }
                    contactsList.clear()
                    contactsList.addAll(contacts)
                    Toast.makeText(context, "Contacts Loaded", Toast.LENGTH_SHORT).show()
                    Log.d("Contacts List", "$contactsList")
                    contactsContinuation?.resume(contactsList)
                }
            } else {
                // Permission denied, handle accordingly (e.g., show a message)
                contactsContinuation?.resume(mutableListOf())
            }
        }
    }

    private suspend fun getContacts(): MutableList<Contact> {
        return withContext(Dispatchers.IO) {
            val contacts = mutableSetOf<Contact>()
            val resolver: ContentResolver = context.contentResolver
            val cursor = resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
            )

            cursor?.use { cursor ->
                val idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneNumberColumnIndex =
                    cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idColumnIndex)
                    val name = cursor.getString(nameColumnIndex)
                    val hasPhoneNumber = cursor.getString(hasPhoneNumberColumnIndex)?.toInt() ?: 0

                    if (hasPhoneNumber > 0) {
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                            arrayOf(id),
                            null
                        )

                        phoneCursor?.use { phoneCursor ->
                            val phoneNumberIndex =
                                phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                            if (phoneNumberIndex == -1) {
                                Log.e("Contacts", "Phone number column not found in phoneCursor")
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
            contacts.toMutableList()
        }
    }
}
