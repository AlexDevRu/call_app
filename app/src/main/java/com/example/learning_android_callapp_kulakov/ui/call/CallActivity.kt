package com.example.learning_android_callapp_kulakov.ui.call

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.example.learning_android_callapp_kulakov.CallManager
import com.example.learning_android_callapp_kulakov.Extensions.isDarkThemeOn
import com.example.learning_android_callapp_kulakov.R
import com.example.learning_android_callapp_kulakov.databinding.ActivityCallBinding
import timber.log.Timber

class CallActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityCallBinding

    private val viewModel by viewModels<CallViewModel>()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        binding.btnAnswer.setOnClickListener(this)
        binding.btnHangup.setOnClickListener(this)

        val phoneNumber = intent?.data?.schemeSpecificPart
        binding.phoneNumber.text = phoneNumber
        if (savedInstanceState == null && phoneNumber != null)
            viewModel.getContact(phoneNumber)

        CallManager.callState.observe(this) {
            Timber.d("CALL_STATE - $it")

            if (it == null) return@observe

            binding.tvDuration.isVisible = it == Call.STATE_ACTIVE || it == Call.STATE_DISCONNECTING || it == Call.STATE_DISCONNECTED

            binding.tvCallStatus.text = when (it) {
                Call.STATE_RINGING -> getString(R.string.ringing)
                Call.STATE_DIALING -> getString(R.string.dialing)
                Call.STATE_ACTIVE -> getString(R.string.active)
                Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> getString(R.string.hanging_up)
                else -> it.toString()
            }

            if (it == Call.STATE_DIALING) {
                binding.root.transitionToEnd()
                //binding.root.jumpToState(R.id.end)
            } else if (it == Call.STATE_ACTIVE) {
                binding.root.transitionToEnd()
                viewModel.start()
            }

            if (it == Call.STATE_DISCONNECTED) {
                binding.btnHangup.isEnabled = false
                binding.btnAnswer.isEnabled = false
                viewModel.stop()
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 1000)
            }
        }

        viewModel.duration.observe(this) {
            val minutes = (it / 60).toString().padStart(2, '0')
            val seconds = (it % 60).toString().padStart(2, '0')
            binding.tvDuration.text = "$minutes:$seconds"
        }

        viewModel.contact.observe(this) {
            if (it != null) {
                val tint = if (binding.root.context.isDarkThemeOn()) Color.WHITE else Color.BLACK
                val unwrappedDrawable = AppCompatResources.getDrawable(binding.root.context, R.drawable.ic_account)
                val wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable!!)
                DrawableCompat.setTint(wrappedDrawable, tint)
                if (it.avatar.isNullOrBlank())
                    binding.avatar.setImageDrawable(wrappedDrawable)
                else
                    Glide.with(binding.avatar)
                        .load(it.avatar)
                        .centerCrop()
                        .error(wrappedDrawable)
                        .into(binding.avatar)
                binding.name.text = it.name
            } else {
                binding.avatar.setImageDrawable(null)
                binding.name.text = null
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