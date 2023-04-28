package com.example.learning_android_callapp_kulakov

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import com.example.learning_android_callapp_kulakov.models.Call
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun getCallsByPhoneNumber(contentResolver: ContentResolver, phoneNumber: String, thumbnail: String?, displayName: String) = withContext(Dispatchers.IO) {
        val regex = Regex("\\D")
        val phoneOnlyDigits = phoneNumber.replace(regex, "")

        val idCol = CallLog.Calls._ID
        val dateCol = CallLog.Calls.DATE
        val numberCol = CallLog.Calls.NUMBER
        val typeCol = CallLog.Calls.TYPE
        val durationCol = CallLog.Calls.DURATION

        val projection = arrayOf(idCol, dateCol, typeCol, durationCol)

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            "$numberCol='$phoneOnlyDigits' or $numberCol='$phoneNumber'",
            null,
            "$dateCol desc"
        ) ?: return@withContext emptyList()

        val idColIdx = cursor.getColumnIndex(idCol)
        val dateColIdx = cursor.getColumnIndex(dateCol)
        val typeColIdx = cursor.getColumnIndex(typeCol)
        val durationColIdx = cursor.getColumnIndex(durationCol)

        val calls = mutableListOf<Call>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColIdx)
            val timestamp = cursor.getLong(dateColIdx)
            val type = cursor.getInt(typeColIdx)
            val duration = cursor.getLong(durationColIdx)

            val lastCall = calls.lastOrNull()
            if (lastCall != null && lastCall.callType == type && lastCall.phoneNumber == displayName) {
                calls.removeLast()
                calls.add(lastCall.copy(count = lastCall.count + 1))
            } else {
                val call = Call(
                    id = id,
                    timestamp = timestamp,
                    phoneNumber = displayName,
                    callType = type,
                    avatar = thumbnail,
                    duration = duration,
                    count = 1
                )
                calls.add(call)
            }
        }

        calls
    }
}