package com.example.learning_android_callapp_kulakov.ui.contact_details

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.learning_android_callapp_kulakov.Utils
import com.example.learning_android_callapp_kulakov.databinding.ActivityContactDetailsBinding
import com.example.learning_android_callapp_kulakov.models.Call
import com.example.learning_android_callapp_kulakov.ui.adapters.CallLogAdapter

class ContactDetailsActivity : AppCompatActivity(), View.OnClickListener, CallLogAdapter.Listener {

    private lateinit var binding: ActivityContactDetailsBinding

    private val viewModel by viewModels<ContactDetailsViewModel>()

    private val callPhonePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            Utils.doCall(this, viewModel.phoneNumber)
        }
    }

    private val callLogAdapter = CallLogAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fabBack.setOnClickListener(this)
        binding.fabCall.setOnClickListener(this)
        binding.fabSms.setOnClickListener(this)
        binding.rvCalls.adapter = callLogAdapter
        observe()
    }

    private fun observe() {
        viewModel.contact.observe(this) {
            binding.tvName.text = it.contact.name
            binding.tvPhoneNumber.text = it.phoneNumber
            Glide.with(binding.ivAvatar)
                .load(it.contact.avatar)
                .into(binding.ivAvatar)
            callLogAdapter.submitList(it.calls)
        }
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.fabBack -> finish()
            binding.fabCall -> callPhonePermissionsLauncher.launch(Manifest.permission.CALL_PHONE)
            binding.fabSms -> Utils.sendSms(this, viewModel.phoneNumber)
        }
    }

    override fun onItemClick(call: Call) {
        callPhonePermissionsLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    companion object {
        private const val CONTACT_ID = "CONTACT_ID"

        fun startActivity(context: Context, contactId: Long) {
            val intent = Intent(context, ContactDetailsActivity::class.java)
            intent.putExtra(CONTACT_ID, contactId)
            context.startActivity(intent)
        }
    }

}