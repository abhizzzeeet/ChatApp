package com.example.chatapp.recyclerView



import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.models.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val messagesList: List<Message>, private val currentUserId: String?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENDER = 1
        private const val VIEW_TYPE_RECEIVER = 2
    }

    override fun getItemViewType(position: Int): Int {
//        Log.d("MessageAdapter senderId","${messagesList[position].senderId}")
//        Log.d("MessageAdapter CurrentUserId","$currentUserId")
        return if (messagesList[position].senderId == currentUserId) VIEW_TYPE_SENDER else VIEW_TYPE_RECEIVER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENDER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sender, parent, false)
            SenderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_receiver, parent, false)
            ReceiverViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messagesList[position]
        if (message != null && message.text.isNotEmpty() && message.senderId.isNotEmpty()) {
            if (holder is SenderViewHolder) {
                holder.bind(message)
            } else if (holder is ReceiverViewHolder) {
                holder.bind(message)
            }
        }

    }

    override fun getItemCount(): Int {
        return messagesList.size
    }

    inner class SenderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val messageTime: TextView = itemView.findViewById(R.id.message_time)
        private val senderName: TextView = itemView.findViewById(R.id.sender_name)

        fun bind(message: Message) {

            messageText.text = message.text
            messageTime.text = formatTime(message.timestamp)
            senderName.visibility = View.VISIBLE
        }
    }

    inner class ReceiverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val messageTime: TextView = itemView.findViewById(R.id.message_time)

        fun bind(message: Message) {
            Log.d("MessageReceiver MessageAdapter","${message.text}")
            Log.d("MessageReceiver MessageAdapter","${message.senderId}")
            Log.d("MessageReceiver MessageAdapter","${message.timestamp}")
            messageText.text = message.text
            messageTime.text = formatTime(message.timestamp)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
