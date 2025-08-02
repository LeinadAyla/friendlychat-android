/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.codelab.friendlychat

import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.codelab.friendlychat.MainActivity.Companion.ANONYMOUS
import com.google.firebase.codelab.friendlychat.databinding.AudioMessageBinding
import com.google.firebase.codelab.friendlychat.databinding.ImageMessageBinding
import com.google.firebase.codelab.friendlychat.databinding.MessageBinding
import com.google.firebase.codelab.friendlychat.databinding.VideoMessageBinding
import com.google.firebase.codelab.friendlychat.model.FriendlyMessage
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

// The FirebaseRecyclerAdapter class and options come from the FirebaseUI library
// See: https://github.com/firebase/FirebaseUI-Android
class FriendlyMessageAdapter(
    private val options: FirebaseRecyclerOptions<FriendlyMessage>,
    private val currentUserName: String?
) : FirebaseRecyclerAdapter<FriendlyMessage, ViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TEXT -> {
                val view = inflater.inflate(R.layout.message, parent, false)
                val binding = MessageBinding.bind(view)
                MessageViewHolder(binding)
            }
            VIEW_TYPE_IMAGE -> {
                val view = inflater.inflate(R.layout.image_message, parent, false)
                val binding = ImageMessageBinding.bind(view)
                ImageMessageViewHolder(binding)
            }
            VIEW_TYPE_AUDIO -> {
                val view = inflater.inflate(R.layout.audio_message, parent, false)
                val binding = AudioMessageBinding.bind(view)
                AudioMessageViewHolder(binding)
            }
            VIEW_TYPE_VIDEO -> {
                val view = inflater.inflate(R.layout.video_message, parent, false)
                val binding = VideoMessageBinding.bind(view)
                VideoMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: FriendlyMessage) {
        val message = options.snapshots[position]
        when (getItemViewType(position)) {
            VIEW_TYPE_TEXT -> (holder as MessageViewHolder).bind(model)
            VIEW_TYPE_IMAGE -> (holder as ImageMessageViewHolder).bind(model)
            VIEW_TYPE_AUDIO -> (holder as AudioMessageViewHolder).bind(model)
            VIEW_TYPE_VIDEO -> (holder as VideoMessageViewHolder).bind(model)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = options.snapshots[position]
        return when {
            message.text != null -> VIEW_TYPE_TEXT
            message.mediaType == "image" -> VIEW_TYPE_IMAGE
            message.mediaType == "audio" -> VIEW_TYPE_AUDIO
            message.mediaType == "video" -> VIEW_TYPE_VIDEO
            else -> VIEW_TYPE_TEXT
        }
    }

    inner class MessageViewHolder(private val binding: MessageBinding) : ViewHolder(binding.root) {
        fun bind(item: FriendlyMessage) {
            binding.messageTextView.text = item.text
            setTextColor(item.name, binding.messageTextView)

            binding.messengerTextView.text = item.name ?: ANONYMOUS
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }

        private fun setTextColor(userName: String?, textView: TextView) {
            if (userName != ANONYMOUS && currentUserName == userName && userName != null) {
                textView.setBackgroundResource(R.drawable.rounded_message_blue)
                textView.setTextColor(Color.WHITE)
            } else {
                textView.setBackgroundResource(R.drawable.rounded_message_gray)
                textView.setTextColor(Color.BLACK)
            }
        }
    }

    inner class ImageMessageViewHolder(private val binding: ImageMessageBinding) :
        ViewHolder(binding.root) {
        fun bind(item: FriendlyMessage) {
            loadImageIntoView(binding.messageImageView, item.imageUrl!!, false)

            binding.messengerTextView.text = item.name ?: ANONYMOUS
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }
    }

    import android.media.MediaPlayer
import android.view.View

// ... existing code ...

    import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView

inner class AudioMessageViewHolder(private val binding: AudioMessageBinding) :
        ViewHolder(binding.root) {
        private var mediaPlayer: MediaPlayer? = null
        private val handler = Handler(Looper.getMainLooper())
        private var isPlaying = false

        private val updateTimeTask = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    val currentPosition = it.currentPosition / 1000
                    val duration = it.duration / 1000
                    binding.audioCurrentTime.text = formatTime(currentPosition)
                    binding.audioTotalTime.text = formatTime(duration)
                    if (isPlaying) {
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }

        fun bind(item: FriendlyMessage) {
            binding.messengerTextView.text = item.name ?: ANONYMOUS
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }

            binding.playAudioButton.setOnClickListener {
                if (!isPlaying) {
                    stopAllAudio() // Pause other audios
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(it.context, Uri.parse(item.imageUrl))
                        prepare()
                        start()
                        isPlaying = true
                        binding.playAudioButton.setImageResource(R.drawable.ic_pause)
                        handler.post(updateTimeTask)
                        setOnCompletionListener {
                            stopAudio()
                        }
                    }
                } else {
                    stopAudio()
                }
            }
        }

        private fun stopAudio() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            binding.playAudioButton.setImageResource(R.drawable.ic_play_arrow)
            handler.removeCallbacks(updateTimeTask)
            binding.audioCurrentTime.text = "00:00"
            binding.audioTotalTime.text = "00:00"
        }

        private fun stopAllAudio() {
            // Notify adapter to stop other audios
            (binding.root.context as? MainActivity)?.stopAllAudioPlayers()
        }

        fun release() {
            stopAudio()
        }

        private fun formatTime(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
    }

    import android.content.Intent
import android.net.Uri
import android.view.View

// ... existing code ...

    import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import android.view.ViewGroup.LayoutParams

inner class VideoMessageViewHolder(private val binding: VideoMessageBinding) :
        ViewHolder(binding.root) {
        private var player: ExoPlayer? = null

        fun bind(item: FriendlyMessage) {
            loadImageIntoView(binding.videoThumbnailImageView, item.imageUrl!!, false)

            binding.messengerTextView.text = item.name ?: ANONYMOUS
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }

            binding.playButton.setOnClickListener {
                if (player == null) {
                    player = ExoPlayer.Builder(binding.root.context).build()
                    binding.videoPlayerView.player = player
                    val mediaItem = MediaItem.fromUri(item.imageUrl!!)
                    player?.setMediaItem(mediaItem)
                    player?.prepare()
                    player?.play()
                    binding.videoThumbnailImageView.visibility = android.view.View.GONE
                    binding.playButton.visibility = android.view.View.GONE
                    binding.videoPlayerView.visibility = android.view.View.VISIBLE
                } else {
                    player?.release()
                    player = null
                    binding.videoThumbnailImageView.visibility = android.view.View.VISIBLE
                    binding.playButton.visibility = android.view.View.VISIBLE
                    binding.videoPlayerView.visibility = android.view.View.GONE
                }
            }
        }

        fun release() {
            player?.release()
            player = null
        }
    }

    private fun loadImageIntoView(view: ImageView, url: String, isCircular: Boolean = true) {
        if (isCircular) {
            Glide.with(view.context)
                .load(url)
                .transform(CircleCrop())
                .into(view)
        } else {
            Glide.with(view.context)
                .load(url)
                .into(view)
        }
    }

    fun stopAllAudioPlayers() {
        for (i in 0 until itemCount) {
            val holder = recyclerView?.findViewHolderForAdapterPosition(i)
            if (holder is AudioMessageViewHolder) {
                holder.release()
            }
        }
    }

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }
        if (url.startsWith("gs://")) {
            val storageReference = Firebase.storage.getReferenceFromUrl(url)
            storageReference.downloadUrl
                .addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    loadWithGlide(view, downloadUrl, isCircular)
                }
                .addOnFailureListener { e ->
                    Log.w(
                        TAG,
                        "Getting download url was not successful.",
                        e
                    )
                }
        } else {
            loadWithGlide(view, url, isCircular)
        }
    }

    private fun loadWithGlide(view: ImageView, url: String, isCircular: Boolean = true) {
        Glide.with(view.context).load(url).into(view)
        var requestBuilder = Glide.with(view.context).load(url)
        if (isCircular) {
            requestBuilder = requestBuilder.transform(CircleCrop())
        }
        requestBuilder.into(view)
    }

    companion object {
        const val TAG = "MessageAdapter"
        const val VIEW_TYPE_TEXT = 1
        const val VIEW_TYPE_IMAGE = 2
        const val VIEW_TYPE_AUDIO = 3
        const val VIEW_TYPE_VIDEO = 4
    }
}
