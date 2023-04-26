package com.example.learning_android_callapp_kulakov

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import timber.log.Timber

class CallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = CallService.YOUR_CHANNEL_ID
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CallService.YOUR_CHANNEL_ID, name, importance)
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }
}