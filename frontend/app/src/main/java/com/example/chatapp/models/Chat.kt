package com.example.chatapp.models

data class Chat(
    var participants: Participants,
    var lastMessage: String,
    var timestamp: Long = 0L
)
data class Participants(
    var userId1: String ,
    var userId2: String
)

