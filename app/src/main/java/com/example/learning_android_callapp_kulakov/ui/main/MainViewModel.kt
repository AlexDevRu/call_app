package com.example.learning_android_callapp_kulakov.ui.main

import android.app.Application
import android.provider.CallLog
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
            val typeCol = CallLog.Calls.TYPE // 1 - Incoming, 2 - Outgoing, 3 - Missed

            val projection = arrayOf(idCol, dateCol, numberCol, durationCol, typeCol)

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

                val call = Call(
                    id = id,
                    timestamp = timestamp,
                    phoneNumber = number,
                    callType = type
                )
                calls.add(call)
            }

            _calls.postValue(calls)

            cursor.close()
        }
    }

}