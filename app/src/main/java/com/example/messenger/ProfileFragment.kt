package com.example.messenger

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    companion object {
        val IMAGE_REQUEST_CODE = 100
    }

    private lateinit var avatar: CircleImageView;
    private lateinit var surnameET: EditText;
    private lateinit var nameET: EditText;
    private lateinit var usernameET: EditText;
    private lateinit var loginET: EditText;
    private lateinit var passwordET: EditText;
    private var uriImg: Uri? = null;
    private lateinit var avatarImg: String;

    val firebaseAuth = FirebaseAuth.getInstance()
    val storageRef = Firebase.storage.reference
    val imageRef = storageRef.child("images/${firebaseAuth.currentUser?.uid}")
    val db = Firebase.firestore
    val usersRef = db.collection("users")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        avatar = view.findViewById(R.id.avatar_image)
        val exit_btn = view.findViewById<Button>(R.id.exit_btn)
        val edit_btn = view.findViewById<Button>(R.id.edit_btn)
        surnameET = view.findViewById(R.id.surnameET)
        nameET = view.findViewById(R.id.nameET)
        usernameET = view.findViewById(R.id.usernameET)
        loginET = view.findViewById(R.id.loginET)
        passwordET = view.findViewById(R.id.passwordET)

        avatar.setOnClickListener{
            pickImageGallery()
        }

        usersRef.document(firebaseAuth.currentUser!!.uid).get()
            .addOnSuccessListener {
                if (it != null){
                    surnameET.setText(it.data?.getValue("surname").toString());
                    nameET.setText(it.data?.getValue("name").toString());
                    usernameET.setText(it.data?.getValue("username").toString());
                    loginET.setText(it.data?.getValue("login").toString());
                    passwordET.setText(it.data?.getValue("password").toString());
                    avatarImg = it.data?.getValue("avatar").toString()
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Загрузка изображения с помощью Picasso и установка его в CircleImageView
                        Picasso.get().load(uri).into(avatar)
                    }.addOnFailureListener {
                        // Обработка ошибки, если не удается получить ссылку на изображение
                    }
                }
            }

        exit_btn.setOnClickListener {
            firebaseAuth.signOut()
            val intent = Intent(
                activity,
                LogActivity::class.java
            )
            startActivity(intent)
            activity?.finish()
        }

        edit_btn.setOnClickListener{
            if (uriImg != null) {
                uploadImageToStorage(uriImg!!)
            } else {
                // Если изображение не было выбрано, просто обновите данные пользователя без загрузки изображения
                updateUserData(avatarImg)
            }
        }

        return view
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK){
            uriImg = data?.data!!
            avatar.setImageURI(uriImg)
        }
    }

    private fun uploadImageToStorage(imageUri: Uri) {
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                Toast.makeText(context, "Изображение загружено", Toast.LENGTH_SHORT).show()
                // Получить ссылку на загруженное изображение
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Обновить данные пользователя в Firestore с новой ссылкой на изображение
                    updateUserData(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка загрузки изображения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserData(imageUri: String) {

        val userData = User(firebaseAuth.currentUser?.uid, loginET.text.toString(), passwordET.text.toString(), surnameET.text.toString(),
            nameET.text.toString(), usernameET.text.toString(), imageUri)

        val userDataMap = userData.toMap()

        usersRef.document(firebaseAuth.currentUser!!.uid)
            .update(userDataMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Данные пользователя изменены", Toast.LENGTH_SHORT).show()
                val user = FirebaseAuth.getInstance().currentUser
                user?.updateEmail(loginET.text.toString())
                user?.updatePassword(passwordET.text.toString())
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка изменения данных: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}