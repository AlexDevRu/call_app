package com.example.learning_android_callapp_kulakov

import android.telecom.Call
import android.telecom.InCallService

class CallService : InCallService() {

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        CallManager.setCall(call)
        CallActivity.start(this, call!!)
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        CallManager.setCall(null)
    }

}