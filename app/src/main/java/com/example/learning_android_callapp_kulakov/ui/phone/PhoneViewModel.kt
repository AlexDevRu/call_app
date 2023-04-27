package com.example.learning_android_callapp_kulakov.ui.phone

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.learning_android_callapp_kulakov.models.Call
import com.example.learning_android_callapp_kulakov.models.PhoneWithThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PhoneViewModel(private val app: Application): AndroidViewModel(app) {

    private val _calls = MutableLiveData<List<Call>>()
    val calls : LiveData<List<Call>> = _calls

    var phoneNumber: String? = null

    private val handler = Handler(Looper.getMainLooper())

    private val callContentObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            Timber.d("call log changed")
            readCallLog()
        }
    }

    init {
        app.contentResolver?.registerContentObserver(
            CallLog.Calls.CONTENT_URI,
            true,
            callContentObserver
        )
    }

    override fun onCleared() {
        super.onCleared()
        app.contentResolver?.unregisterContentObserver(callContentObserver)
    }

    fun readCallLog() {
        viewModelScope.launch(Dispatchers.IO) {
            val idCol = CallLog.Calls._ID
            val dateCol = CallLog.Calls.DATE
            val numberCol = CallLog.Calls.NUMBER
            val durationCol = CallLog.Calls.DURATION
            val typeCol = CallLog.Calls.TYPE

            val contactNumberCol = ContactsContract.CommonDataKinds.Phone.NUMBER
            val displayNameCol = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            val photoThumbnailUriCol = ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI

            val projection = arrayOf(idCol, dateCol, numberCol, durationCol, typeCol)
            val contactProjection = arrayOf(contactNumberCol, displayNameCol, photoThumbnailUriCol)

            val phonesMap = hashMapOf<String, PhoneWithThumbnail>()

            val phonesCursor = app.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, contactProjection,null,null, null
            )
            val regex = Regex("\\D")

            if (phonesCursor != null) {
                val numberIndex = phonesCursor.getColumnIndexOrThrow(contactNumberCol)
                val displayNameIndex = phonesCursor.getColumnIndexOrThrow(displayNameCol)
                val photoThumbnailIndex = phonesCursor.getColumnIndexOrThrow(photoThumbnailUriCol)
                while (phonesCursor.moveToNext()) {
                    val phoneNumber = phonesCursor.getString(numberIndex).replace(regex, "")
                    val name = phonesCursor.getString(displayNameIndex)
                    val thumbnail = phonesCursor.getString(photoThumbnailIndex)
                    phonesMap[phoneNumber] = PhoneWithThumbnail(name, thumbnail)
                }
                phonesCursor.close()
            }

            Timber.d("ASD - $phonesMap")

            val cursor = app.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection, null, null,
                "${CallLog.Calls.DATE} desc"
            ) ?: return@launch

            val idColIdx = cursor.getColumnIndex(idCol)
            val dateColIdx = cursor.getColumnIndex(dateCol)
            val numberColIdx = cursor.getColumnIndex(numberCol)
            val durationColIdx = cursor.getColumnIndex(durationCol)
            val typeColIdx = cursor.getColumnIndex(typeCol)

            val calls = mutableListOf<Call>()

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColIdx)
                val timestamp = cursor.getLong(dateColIdx)
                val number = cursor.getString(numberColIdx)
                val duration = cursor.getString(durationColIdx)
                val type = cursor.getInt(typeColIdx)

                Timber.d("$number $duration $type")

                val phoneNumberOnlyDigits = number.replace(regex, "")

                val displayName = phonesMap[phoneNumberOnlyDigits]?.name ?: number

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
                        avatar = phonesMap[phoneNumberOnlyDigits]?.thumbnail,
                        count = 1
                    )
                    calls.add(call)
                }
            }

            _calls.postValue(calls)

            cursor.close()
        }
    }

}