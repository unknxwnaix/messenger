package com.example.messenger

data class Conversation(
    val members: List<String>? = null,
    val messages: List<Message>? = null

)