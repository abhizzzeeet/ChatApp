package com.example.chatapp.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.chatapp.R

class ChatFragment(private val name: String , private val  phoneNumber: String) : Fragment() {

    private lateinit var nameTextView: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        println("$name , $phoneNumber ")
        val view =inflater.inflate(R.layout.fragment_chat, container, false)

        nameTextView = view.findViewById(R.id.nameTextView)
        nameTextView.text = name
        return view
    }


}