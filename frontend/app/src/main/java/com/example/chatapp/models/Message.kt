package com.example.chatapp.models

data class Message(
    var senderId: String = "",
    var text: String = "",
    var timestamp: Long = 0L
)
