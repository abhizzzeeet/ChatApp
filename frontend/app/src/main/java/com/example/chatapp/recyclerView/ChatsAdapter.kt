package com.example.chatapp.recyclerView

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.contacts.Contact
import com.example.chatapp.contacts.ContactsAdapter
import com.example.chatapp.contacts.OnItemClickListener
import com.example.chatapp.models.PreviousChat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ChatsAdapter(private var previousChats: MutableList<PreviousChat>, private val listener : OnItemClickListener) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {
    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatName: TextView = itemView.findViewById(R.id.chatName)
        val chatPhone: TextView = itemView.findViewById(R.id.chatPhone)
        val chatLastMessage: TextView = itemView.findViewById(R.id.chatLastMessage)
        val inviteButton: Button = itemView.findViewById(R.id.invite_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsAdapter.ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_all_chat, parent, false)
        return ChatsAdapter.ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatsAdapter.ChatViewHolder, position: Int) {

        val previousChat = previousChats[position]
        holder.chatName.text = previousChat.name
        holder.chatPhone.text = previousChat.phoneNumber


        val chatsReference = FirebaseDatabase.getInstance().getReference("chats")
            chatsReference.child(previousChat.chatId).addValueEventListener(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val lastMessage = dataSnapshot.child("lastMessage").getValue(String::class.java)
                    val timestamp = dataSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    previousChat.lastMessage= lastMessage
                    previousChat.lastMessageTimestamp = timestamp
                    holder.chatLastMessage.text = lastMessage
                    // After fetching the timestamp, sort the list and update the adapter
//                    previousChats.sortByDescending { it.lastMessageTimestamp}
//                    Log.d("ChatsAdapter","Before dataset change")
//                    this@ChatsAdapter.notifyDataSetChanged()


                }
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Error fetching last message: ${databaseError.message}")

                }
        })


        holder.inviteButton.visibility = View.GONE

        holder.itemView.setOnClickListener{
            Log.d("ChatsAapter","ItemClicked")
            val currentPosition = holder.adapterPosition
            if (currentPosition != RecyclerView.NO_POSITION) {
                listener.onPreviousChatItemClick(previousChats[currentPosition], currentPosition)
            }

        }
    }

    override fun getItemCount(): Int {
        return previousChats.size
    }


}