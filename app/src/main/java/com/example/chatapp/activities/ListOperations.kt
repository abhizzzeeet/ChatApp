package com.example.chatapp.activities

import android.util.Log
import com.example.chatapp.contacts.Contact
import com.example.chatapp.models.PreviousChat
import com.example.chatapp.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ListOperations(
    private val previousChatList: List<PreviousChat>,
    private val usersList: MutableList<User>,
    private val contactList: MutableList<Contact>
) {

    suspend fun performOperations() = coroutineScope {
        Log.d("ListOperation", "$previousChatList")
        Log.d("ListOperation", "$usersList")

        val removeUsersFromContactListDeferred =
            async(Dispatchers.IO) { removeUsersFromContactList(usersList) }
        removeUsersFromContactListDeferred.await()
        val removePreviousUsersFromUsersListDeferred =
            async(Dispatchers.IO) { removePreviousUsersFromUsersList(previousChatList) }


        removePreviousUsersFromUsersListDeferred.await()
    }


    private suspend fun removeUsersFromContactList(usersList: List<User>) {
        // Implement logic to remove users from contactList based on usersList
        contactList.removeAll { contact: Contact -> usersList.any { it.phoneNumber == contact.phoneNumber } }
        Log.d("NewContactList", "$contactList")
    }

    private suspend fun removePreviousUsersFromUsersList(previousChatList: List<PreviousChat>) {
        // Implement logic to remove users from usersList based on previousChatList
        usersList.removeAll { user: User -> previousChatList.any { it.phoneNumber == user.phoneNumber } }
        Log.d("NewUsersList", "$usersList")
    }


}
