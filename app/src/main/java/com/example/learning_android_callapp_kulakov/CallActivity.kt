package com.example.learning_android_callapp_kulakov

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.learning_android_callapp_kulakov.databinding.ActivityCallBinding

class CallActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        binding.btnAnswer.setOnClickListener(this)
        binding.btnHangup.setOnClickListener(this)

        binding.phoneNumber.text = intent?.data?.schemeSpecificPart

        CallManager.callState.observe(this) {
            binding.btnAnswer.isVisible = it == Call.STATE_RINGING
            binding.btnHangup.isVisible = it == Call.STATE_RINGING || it == Call.STATE_DIALING || it == Call.STATE_ACTIVE
            if (it == Call.STATE_DISCONNECTED) {
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 1000)
            }
        }
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.btnAnswer -> CallManager.answer()
            binding.btnHangup -> CallManager.hangup()
        }
    }

    companion object {
        fun start(context: Context, call: Call) {
            val intent = Intent(context, CallActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(call.details.handle)
            context.startActivity(intent)
        }
    }
}