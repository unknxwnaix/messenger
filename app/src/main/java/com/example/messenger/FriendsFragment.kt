package com.example.messenger

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class FriendsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_friends, container, false)

        val addFriendBtn = view.findViewById<FloatingActionButton>(R.id.add_friend)
        addFriendBtn?.setOnClickListener {
            showAddFriendDialog();
        }

        // Получение списка друзей и заявок в друзья
        val authUser = FirebaseAuth.getInstance().currentUser
        authUser?.uid?.let { userId ->
            FriendHelper().getFriends(userId) { friends ->
                val friendsLayout = view.findViewById<LinearLayout>(R.id.layout_friends)
                friends.forEach { friend ->
                    val friendView = layoutInflater.inflate(R.layout.friend_card, null)
                    val friendName = friendView.findViewById<TextView>(R.id.nameTV)
                    friendName.text = friend.surname + " " + friend.name
                    val friendUserName = friendView.findViewById<TextView>(R.id.usernameTV)
                    friendUserName.text = friend.username
                    val friendAvatar = friendView.findViewById<CircleImageView>(R.id.avatar_image)
                    val friendAvatarRef = Firebase.storage.reference.child("images/${friend.uid}")
                    friendAvatarRef.downloadUrl.addOnSuccessListener { uri ->
                        Picasso.get().load(uri).into(friendAvatar)
                    }.addOnFailureListener {
                        // В случае ошибки, назначить картинку по умолчанию
                        Picasso.get().load(R.drawable.user).into(friendAvatar)
                    }

                    val deleteBtn = friendView.findViewById<Button>(R.id.delete_friend)
                    deleteBtn.setOnClickListener {
                        FriendHelper().deleteFriend(userId, friend.uid)
                    }

                    friendAvatar.setOnClickListener{
                        val intent = Intent(activity, MessageActivity::class.java)
                        intent.putExtra("friend", friend.uid)
                        startActivity(intent)
                    }

                    friendsLayout.addView(friendView)
                }
            }

            FriendHelper().getFriendRequests(userId) { friendRequests ->
                val requestsLayout = view.findViewById<LinearLayout>(R.id.layout_requests)
                friendRequests.forEach { request ->
                    val requestView = layoutInflater.inflate(R.layout.request_card, null)
                    val friendName = requestView.findViewById<TextView>(R.id.nameTV)
                    friendName.text = request.surname + " " + request.name
                    val friendUserName = requestView.findViewById<TextView>(R.id.usernameTV)
                    friendUserName.text = request.username
                    val friendAvatar = requestView.findViewById<CircleImageView>(R.id.avatar_image)
                    val friendAvatarRef = Firebase.storage.reference.child("images/${request.uid}")
                    friendAvatarRef.downloadUrl.addOnSuccessListener { uri ->
                        Picasso.get().load(uri).into(friendAvatar)
                    }.addOnFailureListener {
                        // В случае ошибки, назначить картинку по умолчанию
                        Picasso.get().load(R.drawable.user).into(friendAvatar)
                    }
                    val acceptBtn = requestView.findViewById<Button>(R.id.add_friend)
                    acceptBtn.setOnClickListener {
                        FriendHelper().acceptFriendRequest(userId, request.uid)
                    }
                    val cancelBtn = requestView.findViewById<Button>(R.id.remove_friend)
                    cancelBtn.setOnClickListener {
                        FriendHelper().deleteFriendRequest(userId, request.uid)
                    }

                    requestsLayout.addView(requestView)
                }
            }
        }

        return view
    }

    private fun showAddFriendDialog() {
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(R.layout.dialog_add_friend)

        val layoutUsers = dialog.findViewById<LinearLayout>(R.id.layout_users)

        val db = Firebase.firestore
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val currentUserId = currentUser?.uid.toString()

                    val friend = document.toObject(User::class.java)
                    if (friend.uid != currentUserId) {
                        val userView = layoutInflater.inflate(R.layout.user_item, null)
                        val username = userView.findViewById<TextView>(R.id.user_name)
                        val addFriendBtn = userView.findViewById<Button>(R.id.add_friend_button)

                        FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                            .addOnSuccessListener { currentUserDocument ->
                                if (currentUserDocument != null){
                                    val currentUserData = currentUserDocument.toObject(User::class.java)
                                    username.text = friend.username
                                    addFriendBtn.setOnClickListener {
                                        FriendHelper().sendFriendRequest(currentUserData?.uid.toString(), currentUserData?.username.toString(), currentUserData?.name.toString(), currentUserData?.surname.toString(), currentUserData?.avatar.toString(),
                                            friend.uid.toString(), friend.username.toString(), friend.name.toString(), friend.surname.toString(), friend.avatar.toString())
                                    }
                                    layoutUsers?.addView(userView)
                                }
                            }
                    }
                }

                dialog.show()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting users", exception)
            }

        dialog.show()
    }
}