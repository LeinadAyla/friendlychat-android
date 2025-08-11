package com.google.firebase.codelab.friendlychat

/**
 * Representa uma mensagem do Friendly Chat.
 */
data class FriendlyMessage(
    var text: String? = null,
    var name: String? = null,
    var photoUrl: String? = null,
    var imageUrl: String? = null
)
