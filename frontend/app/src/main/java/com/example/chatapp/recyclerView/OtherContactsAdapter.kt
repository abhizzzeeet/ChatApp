package com.example.chatapp.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.contacts.OnItemClickListener
import com.example.chatapp.models.User


class OtherContactsAdapter(private val users: List<User>, private val listener : OnItemClickListener) : RecyclerView.Adapter<OtherContactsAdapter.OtherContactViewHolder>() {
    class OtherContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatName: TextView = itemView.findViewById(R.id.chatName)
        val chatPhone: TextView = itemView.findViewById(R.id.chatPhone)
        val chatLastMessage: TextView = itemView.findViewById(R.id.chatLastMessage)
        val inviteButton: Button = itemView.findViewById(R.id.invite_button)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OtherContactsAdapter.OtherContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_all_chat, parent, false)
        return OtherContactsAdapter.OtherContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: OtherContactsAdapter.OtherContactViewHolder, position: Int) {
        val user = users[position]
        holder.chatName.text = user.name
        holder.chatPhone.text = user.phoneNumber
        holder.inviteButton.visibility = View.GONE
//        var isUser= contact.isUser
//        if(isUser){
//            holder.inviteButton.visibility = View.GONE
//        }
//        else{
//            holder.inviteButton.visibility = View.VISIBLE
//        }
        holder.itemView.setOnClickListener{
            listener.onOtherContactItemClick(user)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

}