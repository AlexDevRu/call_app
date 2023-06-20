package com.example.learning_android_callapp_kulakov.ui.contact_details

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.learning_android_callapp_kulakov.R
import com.example.learning_android_callapp_kulakov.Utils
import com.example.learning_android_callapp_kulakov.databinding.ActivityContactDetailsBinding
import com.example.learning_android_callapp_kulakov.models.Call
import com.example.learning_android_callapp_kulakov.ui.adapters.CallLogAdapter
import com.example.learning_android_callapp_kulakov.ui.contact_details.qr.QrDialog
import com.example.learning_android_callapp_kulakov.ui.edit.EditActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class ContactDetailsActivity : AppCompatActivity(), View.OnClickListener, CallLogAdapter.Listener, PopupMenu.OnMenuItemClickListener {

    private lateinit var binding: ActivityContactDetailsBinding

    private val viewModel by viewModels<ContactDetailsViewModel>()

    private val popupMenu by lazy {
        PopupMenu(this, binding.btnOptions)
    }

    private val callPhonePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            Utils.doCall(this, viewModel.phoneNumber)
        }
    }

    private val writeContactsPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            viewModel.delete()
        }
    }

    private val callLogAdapter = CallLogAdapter(this)

    private val clipboardManager by lazy {
        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fabBack.setOnClickListener(this)
        binding.fabCall.setOnClickListener(this)
        binding.fabSms.setOnClickListener(this)
        binding.rvCalls.adapter = callLogAdapter

        popupMenu.inflate(R.menu.menu_contact_details)
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.setForceShowIcon(true)

        binding.btnQr.setOnClickListener(this)
        binding.btnEdit.setOnClickListener(this)
        binding.btnOptions.setOnClickListener(this)
        binding.tvName.setOnClickListener(this)
        binding.tvPhoneNumber.setOnClickListener(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        observe()
    }

    override fun onMenuItemClick(menuItem: MenuItem?): Boolean {
        when (menuItem?.itemId) {
            R.id.delete -> writeContactsPermissionsLauncher.launch(Manifest.permission.WRITE_CONTACTS)
            R.id.share -> Utils.shareContact(this, viewModel.lookupKey, viewModel.contactName)
        }
        return true
    }

    private fun observe() {
        viewModel.contact.observe(this) {
            binding.tvName.text = it.contact.name
            binding.tvPhoneNumber.text = if (it.contact.phoneNumber.isNullOrBlank()) "-" else it.contact.phoneNumber

            binding.tvEmail.text = it.email
            binding.tvEmail.isVisible = it.email.isNotBlank()

            binding.tvAddress.text = it.address
            binding.tvAddress.isVisible = it.address.isNotBlank()

            if (it.contact.avatar.isNullOrBlank())
                binding.ivAvatar.setImageResource(R.drawable.ic_account)
            else
                Glide.with(binding.ivAvatar)
                    .load(it.contact.avatar)
                    .centerCrop()
                    .error(R.drawable.ic_account)
                    .into(binding.ivAvatar)

            callLogAdapter.submitList(it.calls)
            binding.tvNoCalls.isVisible = it.calls.isEmpty()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.goBack.collectLatest {
                    finish()
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onClick(view: View?) {
        when (view) {
            binding.fabBack -> finish()
            binding.fabCall -> callPhonePermissionsLauncher.launch(Manifest.permission.CALL_PHONE)
            binding.fabSms -> Utils.sendSms(this, viewModel.phoneNumber)
            binding.btnOptions -> popupMenu.show()
            binding.btnEdit -> EditActivity.startActivity(this, viewModel.contactId)
            binding.tvName, binding.tvPhoneNumber -> {
                val text = (view as TextView).text
                val clip = ClipData.newPlainText("", text)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(applicationContext, getString(R.string.copied_text, text), Toast.LENGTH_SHORT).show()
            }
            binding.btnQr -> {
                val contactInfo = viewModel.getContactInfo()
                val dialog = QrDialog.createInstance(contactInfo)
                dialog.show(supportFragmentManager, null)
            }
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