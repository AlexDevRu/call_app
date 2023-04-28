package com.example.learning_android_callapp_kulakov

import android.content.Context
import android.content.Intent
import android.net.Uri

object Utils {

    fun doCall(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$phoneNumber")
        context.startActivity(intent)
    }

    fun sendSms(context: Context, phoneNumber: String) {
        val uri = Uri.fromParts("sms", phoneNumber, null)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

}