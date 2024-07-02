package com.example.chatapp.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object SharedDataRepository {
    private val messageData = MutableLiveData<String>()

    fun setMessage(message: String) {
        messageData.value = message
        Log.d("SharedDataRepository","${messageData.value}")
    }

    fun getMessage(): LiveData<String> {
        Log.d("SharedDataRepository","$messageData")
        return messageData
    }
}