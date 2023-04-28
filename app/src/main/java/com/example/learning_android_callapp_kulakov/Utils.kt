package com.example.learning_android_callapp_kulakov

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
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

        cursor.close()

        calls
    }

    fun shareContact(context: Context, lookupKey: String, contactName: String) {
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = ContactsContract.Contacts.CONTENT_VCARD_TYPE
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.putExtra(Intent.EXTRA_SUBJECT, contactName)
        context.startActivity(intent)
    }

    fun getPhoneNumbersForContacts(context: Context) : Map<Long, String?> {
        val idCol = ContactsContract.CommonDataKinds.Phone.CONTACT_ID
        val contactNumberCol = ContactsContract.CommonDataKinds.Phone.NUMBER

        val projection = arrayOf(idCol, contactNumberCol)

        val phonesMap = hashMapOf<Long, String?>()

        val phonesCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection,null,null, null
        ) ?: return phonesMap

        val idIndex = phonesCursor.getColumnIndexOrThrow(idCol)
        val numberIndex = phonesCursor.getColumnIndexOrThrow(contactNumberCol)

        while (phonesCursor.moveToNext()) {
            val phoneNumber = phonesCursor.getString(numberIndex)
            val contactId = phonesCursor.getLong(idIndex)
            phonesMap[contactId] = phoneNumber
        }

        phonesCursor.close()

        return phonesMap
    }
}