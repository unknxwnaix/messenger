    package com.example.messenger

    data class Message(
        val text: String? = null,
        val senderId: String? = null,
        val receiverId: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )