package com.example.learning_android_callapp_kulakov.models

data class ContactDetails(
    val contact: Contact,
    val phoneNumber: String,
    val calls: List<Call>
)