package com.example.learning_android_callapp_kulakov

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import androidx.core.app.NotificationCompat


class CallService : InCallService() {

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)

        CallManager.setCall(call)

        /*val myKM = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (myKM.isDeviceLocked) {
            CallActivity.start(this, call!!)
        } else {
            postNotification(call!!)
        }*/

        CallActivity.start(this, call!!)
    }

    private fun postNotification(call: Call) {
        val intent = Intent(this, CallActivity::class.java)
            .setData(call.details.handle)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        NotificationCompat.Builder(this, YOUR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${call.details.callerDisplayName} calling")
            .setContentText("Tap to answer call")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()
            .let {
                getSystemService(NotificationManager::class.java)
                    .notify(1, it)
            }
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        CallManager.setCall(null)
        getSystemService(NotificationManager::class.java).cancel(1)
    }

    companion object {
        const val YOUR_CHANNEL_ID = "Main"
    }

}