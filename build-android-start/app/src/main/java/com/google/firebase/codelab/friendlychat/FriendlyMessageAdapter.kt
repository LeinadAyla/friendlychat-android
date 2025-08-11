package com.google.firebase.codelab.friendlychat

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.codelab.friendlychat.databinding.ItemMessageBinding

/**
 * Este adaptador utiliza a biblioteca FirebaseUI para se ligar aos dados do
 * Firebase Realtime Database. Ele gerencia o ciclo de vida do listener de
 * dados e as atualizações da UI automaticamente.
 */
class FriendlyMessageAdapter(
    private val options: FirebaseRecyclerOptions<FriendlyMessage>
) : FirebaseRecyclerAdapter<FriendlyMessage, FriendlyMessageAdapter.MessageViewHolder>(options) {

    /**
     * O ViewHolder contém as referências às views para um único item de mensagem.
     * Utiliza o View Binding para acessar as views de forma segura.
     */
    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: FriendlyMessage) {
            // Se a mensagem contém texto, exibe o TextView e oculta o ImageView.
            if (model.text != null) {
                binding.messageTextView.text = model.text
                binding.messageTextView.visibility = TextView.VISIBLE
                binding.messageImageView.visibility = ImageView.GONE
            }
            // Se a mensagem contém uma URL de imagem, carrega a imagem usando Glide e oculta o TextView.
            else if (model.imageUrl != null) {
                binding.messageTextView.visibility = TextView.GONE
                binding.messageImageView.visibility = ImageView.VISIBLE
                Glide.with(itemView.context)
                    .load(model.imageUrl)
                    .into(binding.messageImageView)
            }
            // Caso contrário, oculta ambas as views.
            else {
                binding.messageTextView.visibility = TextView.GONE
                binding.messageImageView.visibility = ImageView.GONE
            }

            // Define o nome do remetente, usando a constante da classe MainActivity.
            binding.messengerTextView.text = model.name ?: MainActivity.ANONYMOUS
        }
    }

    /**
     * Chamado para criar um novo ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMessageBinding.inflate(inflater, parent, false)
        return MessageViewHolder(binding)
    }

    /**
     * Chamado para ligar os dados de um objeto FriendlyMessage a um ViewHolder.
     * A anotação `override` é essencial aqui.
     */
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int, model: FriendlyMessage) {
        holder.bind(model)
    }
}
