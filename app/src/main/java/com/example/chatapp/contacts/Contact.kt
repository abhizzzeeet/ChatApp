package com.example.chatapp.contacts

data class Contact(
    val name: String,
    val phoneNumber: String,
    val isUser: Boolean,
    val userId: String?
)
