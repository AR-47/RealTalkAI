package com.example.realtalkai

enum class Sender {
    USER, AI
}

data class ChatMessage(
    val text: String,
    val sender: Sender
)