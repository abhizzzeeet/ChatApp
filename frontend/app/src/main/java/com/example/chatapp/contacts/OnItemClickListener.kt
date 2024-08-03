package com.example.chatapp.contacts

import com.example.chatapp.models.PreviousChat
import com.example.chatapp.models.User

interface OnItemClickListener {
    fun onPreviousChatItemClick(previousChat: PreviousChat,position: Int)
    fun onOtherContactItemClick(user: User)
    fun onInviteItemClick(contact: Contact)
}