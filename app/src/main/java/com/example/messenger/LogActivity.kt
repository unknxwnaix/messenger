package com.example.messenger

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LogActivity : AppCompatActivity() {

    private var firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        var loginInput = findViewById<EditText>(R.id.login_input)
        var passwordInput = findViewById<EditText>(R.id.password_input)

        var regBtn = findViewById<Button>(R.id.reg_btn)
        var enterBtn = findViewById<Button>(R.id.auth_btn)
        regBtn.setOnClickListener(View.OnClickListener {
            val intent = Intent(
                this@LogActivity,
                RegActivity::class.java
            )
            startActivity(intent)
        })

        enterBtn.setOnClickListener(View.OnClickListener {
            if (loginInput.getText().toString().isNotEmpty() && passwordInput.getText().toString().isNotEmpty()
            ) {
                val email = loginInput.getText().toString().trim { it <= ' ' }
                val password = passwordInput.getText().toString().trim { it <= ' ' }
                authUser(email, password)
            } else Toast.makeText(
                this@LogActivity,
                "Поля не должны быть пустыми",
                Toast.LENGTH_SHORT
            ).show()
        })

        if (firebaseAuth.currentUser != null) {
            val intent: Intent = Intent(this@LogActivity, MessengerActivity::class.java)
            intent.putExtra("whatAccount", firebaseAuth.currentUser!!.displayName)
            intent.putExtra("whatEmail", firebaseAuth.currentUser!!.email)
            startActivity(intent)
            finish()
        }

    }


    private fun authUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val intent = Intent(
                    this@LogActivity,
                    MessengerActivity::class.java
                )
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@LogActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}