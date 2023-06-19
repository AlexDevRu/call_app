package com.example.learning_android_callapp_kulakov.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactInfo(
    val givenName: String,
    val familyName: String,
    val middleName: String,
    val email: String,
    val address: String,
    val phoneNumber: String?
) : Parcelable
