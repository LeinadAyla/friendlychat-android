package com.google.firebase.codelab.friendlychat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.codelab.friendlychat.databinding.ImageMessageBinding
import com.google.firebase.codelab.friendlychat.databinding.MessageBinding

class FriendlyMessageAdapter(
    options: FirebaseRecyclerOptions<FriendlyMessage>,
    private val userName: String?
) : FirebaseRecyclerAdapter<FriendlyMessage, RecyclerView.ViewHolder>(options) {

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).imageUrl != null) VIEW_TYPE_IMAGE else VIEW_TYPE_TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_TEXT) {
            val binding = MessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MessageViewHolder(binding)
        } else {
            val binding = ImageMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ImageMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, model: FriendlyMessage) {
        when (holder) {
            is MessageViewHolder -> holder.bind(model)
            is ImageMessageViewHolder -> holder.bind(model)
        }
    }

    inner class MessageViewHolder(private val binding: MessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendlyMessage) {
            binding.messageTextView.text = item.text
            binding.messengerTextView.text = item.name ?: MainActivity.ANONYMOUS
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl!!)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }
    }

    inner class ImageMessageViewHolder(private val binding: ImageMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendlyMessage) {
            loadImageIntoView(binding.messageImageView, item.imageUrl!!)
            binding.messengerTextView.text = item.name ?: MainActivity.ANONYMOUS
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl!!)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_TEXT = 1
        private const val VIEW_TYPE_IMAGE = 2
    }
}
