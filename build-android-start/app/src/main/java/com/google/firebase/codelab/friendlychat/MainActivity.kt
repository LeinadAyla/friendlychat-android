package com.google.firebase.codelab.friendlychat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.codelab.friendlychat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var manager: LinearLayoutManager
    private lateinit var adapter: FriendlyMessageAdapter

    // Firebase
    private lateinit var auth: com.google.firebase.auth.FirebaseAuth
    private lateinit var db: com.google.firebase.database.FirebaseDatabase

    private val openDocument = registerForActivityResult(MyOpenDocumentContract()) { uri ->
        uri?.let { onImageSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // usar emuladores em DEBUG
        if (BuildConfig.DEBUG) {
            Firebase.database.useEmulator("10.0.2.2", 9000)
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.storage.useEmulator("10.0.2.2", 9199)
        }

        // inicializa auth
        auth = Firebase.auth

        // Se não há usuário logado, redireciona para o SignIn.
        if (auth.currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Se usuário logado, segue com a inicialização do chat.
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔄 Configurar Realtime Database + RecyclerView
        db = Firebase.database
        val messagesRef = db.reference.child(MESSAGES_CHILD)

        val options = FirebaseRecyclerOptions.Builder<FriendlyMessage>()
            .setQuery(messagesRef, FriendlyMessage::class.java)
            .setLifecycleOwner(this)
            .build()

        adapter = FriendlyMessageAdapter(options)

        binding.progressBar.visibility = ProgressBar.INVISIBLE

        manager = LinearLayoutManager(this)
        manager.stackFromEnd = true
        binding.messageRecyclerView.layoutManager = manager
        binding.messageRecyclerView.adapter = adapter

        adapter.registerAdapterDataObserver(
            MyScrollToBottomObserver(binding.messageRecyclerView, adapter, manager)
        )

        // 📝 Texto
        binding.messageEditText.addTextChangedListener(MyButtonObserver(binding.sendButton))
        binding.sendButton.setOnClickListener {
            val msg = FriendlyMessage(
                binding.messageEditText.text.toString(),
                getUserName(),
                getPhotoUrl(),
                null
            )
            db.reference.child(MESSAGES_CHILD).push().setValue(msg)
            binding.messageEditText.setText("")
        }

        // 📷 Imagem
        binding.addMessageImageView.setOnClickListener {
            openDocument.launch(arrayOf("image/*"))
        }
    }

    override fun onStart() {
        super.onStart()
        // O adapter.startListening() é chamado automaticamente pelo setLifecycleOwner
        // se a atividade for a proprietária do ciclo de vida, então não é necessário aqui.
    }

    override fun onStop() {
        super.onStop()
        // O adapter.stopListening() é chamado automaticamente pelo setLifecycleOwner.
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getPhotoUrl(): String? {
        val user = auth.currentUser
        return user?.photoUrl?.toString()
    }

    private fun getUserName(): String? {
        val user = auth.currentUser
        return user?.displayName ?: ANONYMOUS
    }

    private fun onImageSelected(uri: Uri) {
        Log.d(TAG, "Uri: $uri")
        val user = auth.currentUser
        val tempMessage = FriendlyMessage(null, getUserName(), getPhotoUrl(), LOADING_IMAGE_URL)

        db.reference.child(MESSAGES_CHILD)
            .push()
            .setValue(tempMessage) { error, ref ->
                if (error != null) {
                    Log.w(TAG, "❌ Erro ao gravar msg temporária", error.toException())
                    return@setValue
                }
                val key = ref.key
                val storageRef = Firebase.storage.getReference(user!!.uid)
                    .child(key!!)
                    .child(uri.lastPathSegment!!)
                putImageInStorage(storageRef, uri, key)
            }
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String?) {
        storageReference.putFile(uri)
            .addOnSuccessListener(this) { taskSnapshot ->
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val msg = FriendlyMessage(null, getUserName(), getPhotoUrl(), downloadUri.toString())
                        db.reference.child(MESSAGES_CHILD).child(key!!).setValue(msg)
                    }
            }
            .addOnFailureListener(this) {
                Log.w(TAG, "❌ Falha no upload de imagem", it)
            }
    }

    private fun signOut() {
        AuthUI.getInstance().signOut(this)
        startActivity(Intent(this, SignInActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "MainActivity"
        const val MESSAGES_CHILD = "messages"
        const val ANONYMOUS = "anonymous"
        private const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    }
}
