package com.example.messenger

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class MessagesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_messages, container, false)

        FriendHelper().getFriends(FirebaseAuth.getInstance().currentUser!!.uid) { friends ->
            val chatLayout = view.findViewById<LinearLayout>(R.id.layout_chats)
            friends.forEach { friend ->
                val chatView = inflater.inflate(R.layout.chat, null)
                val chatName = chatView.findViewById<TextView>(R.id.nameTV)
                chatName.text = friend.surname + " " + friend.name

                val friendAvatar = chatView.findViewById<CircleImageView>(R.id.avatar_image)
                val friendAvatarRef = Firebase.storage.reference.child("images/${friend.uid}")
                friendAvatarRef.downloadUrl.addOnSuccessListener { uri ->
                    Picasso.get().load(uri).into(friendAvatar)
                }.addOnFailureListener {
                    // В случае ошибки, назначить картинку по умолчанию
                    Picasso.get().load(R.drawable.user).into(friendAvatar)
                }

                chatView.setOnClickListener {
                    val intent = Intent(activity, MessageActivity::class.java)
                    intent.putExtra("friend", friend.uid)
                    startActivity(intent)
                }

                chatView.setOnLongClickListener {
                    // Handle the long press action here
                    val options = arrayOf("Удалить?")

                    val builder = AlertDialog.Builder(context)
                    builder.setTitle("Удаление чата")
                    builder.setItems(options) { dialog, which ->
                        when (which) {
                            0 -> {
                                FriendHelper().deleteConversations(FirebaseAuth.getInstance().currentUser!!.uid, friend.uid)
                            }
                        }
                    }
                    builder.show()
                    true // Return true to indicate that the long click event has been consumed
                }

                chatLayout.addView(chatView)
            }
        }

        return view;
    }
}