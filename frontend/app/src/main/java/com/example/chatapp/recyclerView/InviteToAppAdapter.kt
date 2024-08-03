package com.example.chatapp.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.contacts.Contact
import com.example.chatapp.contacts.OnItemClickListener



class InviteToAppAdapter(private val contacts: List<Contact>, private val listener : OnItemClickListener) : RecyclerView.Adapter<InviteToAppAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatName: TextView = itemView.findViewById(R.id.chatName)
        val chatPhone: TextView = itemView.findViewById(R.id.chatPhone)
        val chatLastMessage: TextView = itemView.findViewById(R.id.chatLastMessage)
        val inviteButton: Button = itemView.findViewById(R.id.invite_button)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteToAppAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_all_chat, parent, false)
        return InviteToAppAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: InviteToAppAdapter.ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.chatName.text = contact.name
        holder.chatPhone.text = contact.phoneNumber
        holder.inviteButton.visibility = View.VISIBLE
//        var isUser= contact.isUser
//        if(isUser){
//            holder.inviteButton.visibility = View.GONE
//        }
//        else{
//            holder.inviteButton.visibility = View.VISIBLE
//        }
        holder.itemView.setOnClickListener{
            listener.onInviteItemClick(contact)
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

}