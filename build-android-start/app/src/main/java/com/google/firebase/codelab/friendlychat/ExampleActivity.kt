package com.google.firebase.codelab.friendlychat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class ExampleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        val btnNext = findViewById<Button>(R.id.btn_next)
        btnNext.setOnClickListener {
            startActivity(Intent(this, SecondExampleActivity::class.java))
        }
    }
}
