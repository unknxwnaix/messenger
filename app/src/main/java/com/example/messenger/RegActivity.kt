package com.example.messenger

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegActivity : AppCompatActivity() {
    private var firebaseAuth = FirebaseAuth.getInstance()
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var userRef = db.collection("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reg)

        var loginInput = findViewById<EditText>(R.id.login_input)
        var passwordInput = findViewById<EditText>(R.id.password_input)
        var surnameInput = findViewById<EditText>(R.id.surname_input)
        var nameInput = findViewById<EditText>(R.id.name_input)
        var usernameInput = findViewById<EditText>(R.id.username_input)
        var regBtn = findViewById<Button>(R.id.reg_btn)
        regBtn.setOnClickListener(View.OnClickListener {
            if (loginInput.text.isNotEmpty() && passwordInput.text.isNotEmpty()
                && surnameInput.text.isNotEmpty() && nameInput.text.isNotEmpty() && usernameInput.text.isNotEmpty()
            ) {
                val email: String = loginInput.getText().toString().trim { it <= ' ' }
                val password: String = passwordInput.getText().toString().trim { it <= ' ' }
                val surname: String = surnameInput.getText().toString().trim { it <= ' ' }
                val name: String = nameInput.getText().toString().trim { it <= ' ' }
                val username: String = usernameInput.getText().toString().trim { it <= ' ' }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    loginInput.error = "Неверный формат почты"
                } else if (password.length < 6) {
                    passwordInput.error = "Пароль слишком короткий"
                } else {
                    regUser(email, password, surname, name, username)
                }
            } else Toast.makeText(
                this@RegActivity,
                "Поля не должны быть пустыми",
                Toast.LENGTH_SHORT
            ).show()
        })

    }

    private fun regUser(email: String, password: String, surname: String, name: String, username: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val currentUser = firebaseAuth.currentUser
                userRef.document(currentUser!!.uid).set(User(currentUser!!.uid, currentUser.email, password, surname, name, username, "none"))
                    .addOnSuccessListener {
                        val intent = Intent(this@RegActivity, MessengerActivity::class.java)
                        startActivity(intent)
                        finish()
                        Toast.makeText(
                            this@RegActivity,
                            "Пользователь " + currentUser.email + " зарегистрирован успешно",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this@RegActivity,
                            e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@RegActivity,
                    e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}