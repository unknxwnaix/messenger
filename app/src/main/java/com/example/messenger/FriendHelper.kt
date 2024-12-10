package com.example.messenger

import Friend
import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class FriendHelper {
    private val db = FirebaseFirestore.getInstance()

    fun sendFriendRequest(senderId: String, senderUsername: String, senderName: String, senderSurname: String, senderAvatar: String, receiverId: String, receiverUsername: String, receiverName: String, receiverSurname: String, receiverAvatar: String) {
        val senderFriend = Friend(senderId, senderUsername, "pending", senderSurname, senderName, senderAvatar)
        val receiverFriend = Friend(receiverId, receiverUsername, "pending", receiverSurname, receiverName, receiverAvatar)

        // Saving sender information in the friend request document
        db.collection("friends").document(receiverId).collection("incomingRequests").document(senderId).set(senderFriend)
        // Saving receiver information in the friend request document
        db.collection("friends").document(senderId).collection("outgoingRequests").document(receiverId).set(receiverFriend)

        // Adding friends to the friends collection if the request is accepted
        db.collection("friends").document(senderId).collection("friends").document(receiverId).set(receiverFriend)
        db.collection("friends").document(receiverId).collection("friends").document(senderId).set(senderFriend)
    }

    fun deleteFriendRequest(userId: String, friendId: String) {
        db.collection("friends").document(userId).collection("incomingRequests").document(friendId).delete()
        db.collection("friends").document(friendId).collection("outgoingRequests").document(userId).delete()
    }

    fun acceptFriendRequest(userId: String, friendId: String) {
        db.collection("friends").document(userId).collection("friends").document(friendId).update("status", "accepted")
        db.collection("friends").document(friendId).collection("friends").document(userId).update("status", "accepted")

        // Delete friend request after it has been accepted
        db.collection("friends").document(userId).collection("incomingRequests").document(friendId).delete()
        db.collection("friends").document(friendId).collection("outgoingRequests").document(userId).delete()
    }

    fun deleteFriend(userId: String, friendId: String) {
        db.collection("friends").document(userId).collection("friends").document(friendId).delete()
        db.collection("friends").document(friendId).collection("friends").document(userId).delete()
    }

    fun getFriends(userId: String, friendListCallback: (List<Friend>) -> Unit) {
        db.collection("friends").document(userId).collection("friends")
            .whereEqualTo("status", "accepted")
            .get()
            .addOnSuccessListener { result ->
                val friends = result.toObjects(Friend::class.java)
                friendListCallback(friends)
            }
    }

    fun getFriendRequests(userId: String, friendListCallback: (List<Friend>) -> Unit) {
        db.collection("friends").document(userId).collection("incomingRequests")
            .get()
            .addOnSuccessListener { result ->
                val friends = result.toObjects(Friend::class.java)
                friendListCallback(friends)
            }
    }

    fun sendMessageToFriend(senderId: String, receiverId: String, text: String) {
        val message = Message(text, senderId, receiverId, System.currentTimeMillis())

        db.collection("conversations").document("$senderId").collection("chats").document("$receiverId").collection("messages").add(message)
        db.collection("conversations").document("$receiverId").collection("chats").document("$senderId").collection("messages").add(message)
    }

    fun getMessagesForSender(senderId: String, receiverId: String, messageListCallback: (List<Message>) -> Unit) {
        db.collection("conversations").document(senderId).collection("chats").document(receiverId).collection("messages")
            .get()
            .addOnSuccessListener { result ->
                val messages = result.toObjects(Message::class.java)
                val sortedMessages = messages.sortedBy { it.timestamp } // Сортировка сообщений по timestamp
                messageListCallback(sortedMessages)
            }
    }

    fun deleteMessageByContent(senderId: String, receiverId: String, text: String) {
        db.collection("conversations").document(senderId).collection("chats").document(receiverId).collection("messages")
            .whereEqualTo("text", text)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    doc.reference.delete()
                }
            }
    }

    fun deleteMessageByContentAll(senderId: String, receiverId: String, text: String) {
        db.collection("conversations").document(senderId).collection("chats").document(receiverId).collection("messages")
            .whereEqualTo("text", text)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    doc.reference.delete()
                }
            }

        db.collection("conversations").document(receiverId).collection("chats").document(senderId).collection("messages")
            .whereEqualTo("text", text)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    doc.reference.delete()
                }
            }
    }

    fun deleteConversations(userId: String, receiverId: String) {
        db.collection("conversations").document(userId).collection("chats").document(receiverId).collection("messages")
            .get() .addOnSuccessListener { result -> for (document in result.documents)
            {
                db.collection("conversations").document(userId).collection("chats").document(receiverId).collection("messages").document(document.id).delete()
            }
            }
    }

}
