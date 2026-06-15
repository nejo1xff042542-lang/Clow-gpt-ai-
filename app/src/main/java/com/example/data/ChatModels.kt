package com.example.data

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val role: String = "user",
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val updatedAt: Long = System.currentTimeMillis()
)
