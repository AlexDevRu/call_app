package com.example.learning_android_callapp_kulakov.ui.edit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.learning_android_callapp_kulakov.R
import com.example.learning_android_callapp_kulakov.databinding.ActivityEditContactBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditActivity : AppCompatActivity(), View.OnClickListener, FragmentResultListener {

    private lateinit var binding: ActivityEditContactBinding

    private val viewModel by viewModels<EditContactViewModel>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            saveChanges()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabSave.setOnClickListener(this)
        binding.ivAvatar.setOnClickListener(this)
        binding.fabBack.setOnClickListener(this)

        supportFragmentManager.setFragmentResultListener(GetPhotoDialog.requestKey, this, this)

        observe()
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.getParcelable(GetPhotoDialog.URI, Uri::class.java)
        } else {
            result.get(GetPhotoDialog.URI) as Uri
        }
        if (uri != null)
            viewModel.setUri(uri)
    }

    private fun observe() {
        viewModel.contact.observe(this) {
            binding.etGivenName.setText(it.givenName)
            binding.etFamilyName.setText(it.familyName)
            binding.etMiddleName.setText(it.middleName)
            binding.etPhone.setText(it.contact.phoneNumber)
            binding.etEmail.setText(it.email)
            binding.etAddress.setText(it.address)
            if (it.contact.avatar.isNullOrBlank())
                binding.ivAvatar.setImageResource(R.drawable.ic_account)
            else
                Glide.with(binding.ivAvatar)
                    .load(it.contact.avatar)
                    .centerCrop()
                    .error(R.drawable.ic_account)
                    .into(binding.ivAvatar)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.goBack.collectLatest {
                    finish()
                }
            }
        }
        viewModel.uri.observe(this) {
            Glide.with(binding.ivAvatar)
                .load(it)
                .into(binding.ivAvatar)
        }
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.fabBack -> finish()
            binding.fabSave -> permissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
            binding.ivAvatar -> {
                val getPhotoDialog = GetPhotoDialog()
                getPhotoDialog.show(supportFragmentManager, null)
            }
        }
    }

    private fun saveChanges() {
        val givenName = binding.etGivenName.text?.toString().orEmpty()
        val familyName = binding.etFamilyName.text?.toString().orEmpty()
        val middleName = binding.etMiddleName.text?.toString().orEmpty()
        val phone = binding.etPhone.text?.toString().orEmpty()
        val email = binding.etEmail.text?.toString().orEmpty()
        val address = binding.etAddress.text?.toString().orEmpty()
        viewModel.saveChanges(givenName, familyName, middleName, phone, email, address)
    }

    companion object {
        fun startActivity(context: Context, contactId: Long) {
            val intent = Intent(context, EditActivity::class.java)
                .putExtra(EditContactViewModel.CONTACT_ID, contactId)
            context.startActivity(intent)
        }
    }
}