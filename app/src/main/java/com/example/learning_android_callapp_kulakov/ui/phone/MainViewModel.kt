package com.example.learning_android_callapp_kulakov.ui.phone

import android.app.Application
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.learning_android_callapp_kulakov.models.Call
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(private val app: Application): AndroidViewModel(app) {

    private val _calls = MutableLiveData<List<Call>>()
    val calls : LiveData<List<Call>> = _calls

    var phoneNumber: String? = null

    fun readCallLog() {
        viewModelScope.launch(Dispatchers.IO) {
            val idCol = CallLog.Calls._ID
            val dateCol = CallLog.Calls.DATE
            val numberCol = CallLog.Calls.NUMBER
            val durationCol = CallLog.Calls.DURATION
            val typeCol = CallLog.Calls.TYPE

            val projection = arrayOf(idCol, dateCol, numberCol, durationCol, typeCol)

            val phonesMap = hashMapOf<String, String>()

            val phonesCursor = app.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null
            )
            val regex = Regex("\\D")

            if (phonesCursor != null) {
                val numberIndex = phonesCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val displayNameIndex = phonesCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                while (phonesCursor.moveToNext()) {
                    val phoneNumber = phonesCursor.getString(numberIndex).replace(regex, "")
                    val name = phonesCursor.getString(displayNameIndex)
                    phonesMap[phoneNumber] = name
                }
                phonesCursor.close()
            }

            Timber.d("ASD - ${phonesMap}")

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

                val call = Call(
                    id = id,
                    timestamp = timestamp,
                    phoneNumber = phonesMap[phoneNumberOnlyDigits] ?: number,
                    callType = type
                )
                calls.add(call)
            }

            _calls.postValue(calls)

            cursor.close()
        }
    }

}