package com.example.learning_android_callapp_kulakov.models

data class Call(
    val id: Long,
    val timestamp: Long,
    val callType: Int,
    val phoneNumber: String,
    val avatar: String?,
    val duration: Long,
    val count: Int
)
