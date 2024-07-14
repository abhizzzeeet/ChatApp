package com.example.chatapp.recyclerView

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

class ChatsAdapter(private val previousChats: List<PreviousChat>, private val listener : OnItemClickListener) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {
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
        holder.chatLastMessage.text = previousChat.lastMessage
        holder.inviteButton.visibility = View.GONE
//        var isUser= contact.isUser
//        if(isUser){
//            holder.inviteButton.visibility = View.GONE
//        }
//        else{
//            holder.inviteButton.visibility = View.VISIBLE
//        }
        holder.itemView.setOnClickListener{
            listener.onPreviousChatItemClick(previousChat)
        }
    }

    override fun getItemCount(): Int {
        return previousChats.size
    }

}