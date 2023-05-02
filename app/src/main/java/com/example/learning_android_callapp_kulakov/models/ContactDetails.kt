package com.example.learning_android_callapp_kulakov.models

data class ContactDetails(
    val contact: Contact,
    val givenName: String,
    val familyName: String,
    val middleName: String,
    val email: String,
    val address: String,
    val calls: List<Call>
)
