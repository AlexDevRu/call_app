package com.example.learning_android_callapp_kulakov

import android.os.Build
import android.telecom.Call
import android.telecom.VideoProfile
import androidx.lifecycle.MutableLiveData

object CallManager {
    private var call: Call? = null

    var callState = MutableLiveData<Int>()

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            super.onStateChanged(call, state)
            callState.value = state
        }
    }

    fun answer() {
        call?.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun hangup() {
        call?.disconnect()
    }

    fun setCall(value: Call?) {
        call?.unregisterCallback(callback)
        value?.registerCallback(callback)
        callState.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            value?.details?.state
        else
            value?.state
        call = value
    }
}