package com.example.learning_android_callapp_kulakov.ui.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.learning_android_callapp_kulakov.Extensions.isDarkThemeOn
import com.example.learning_android_callapp_kulakov.R
import com.example.learning_android_callapp_kulakov.databinding.ItemCallBinding
import com.example.learning_android_callapp_kulakov.models.Call
import java.text.SimpleDateFormat
import java.util.*

class CallLogAdapter(
    private val listener: Listener
): ListAdapter<Call, CallLogAdapter.CallViewHolder>(DIFF_UTIL) {

    companion object {
        val DIFF_UTIL = object : ItemCallback<Call>() {
            override fun areItemsTheSame(oldItem: Call, newItem: Call): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Call, newItem: Call): Boolean {
                return oldItem == newItem
            }
        }
    }

    interface Listener {
        fun onItemClick(call: Call)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val binding = ItemCallBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CallViewHolder(
        private val binding: ItemCallBinding
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        private var call: Call? = null

        init {
            binding.root.setOnClickListener(this)
        }

        @SuppressLint("SetTextI18n")
        fun bind(call: Call) {
            this.call = call

            val tint = if (binding.root.context.isDarkThemeOn()) Color.WHITE else Color.BLACK
            val unwrappedDrawable = AppCompatResources.getDrawable(binding.root.context, R.drawable.ic_account)
            val wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable!!)
            DrawableCompat.setTint(wrappedDrawable, tint)

            Glide.with(binding.ivAvatar)
                .load(call.avatar)
                .error(wrappedDrawable)
                .into(binding.ivAvatar)

            binding.tvPhoneNumber.text = if (call.count > 1)
                binding.root.context.getString(R.string.call_log_title_with_counter, call.phoneNumber, call.count)
            else
                call.phoneNumber

            binding.tvDate.text = simpleDateFormat.format(call.timestamp)

            binding.ivCallType.setImageResource(
                when (call.callType) {
                    CallLog.Calls.INCOMING_TYPE -> R.drawable.ic_call_received
                    CallLog.Calls.OUTGOING_TYPE -> R.drawable.ic_call_made
                    else -> R.drawable.ic_call_missed
                }
            )
        }

        override fun onClick(view: View?) {
            when (view) {
                binding.root -> listener.onItemClick(call!!)
            }
        }
    }

}