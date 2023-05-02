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

        observe()

        supportFragmentManager.setFragmentResultListener(GetPhotoDialog.requestKey, this, this)
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.getParcelable(GetPhotoDialog.URI, Uri::class.java)
        } else {
            result.get(GetPhotoDialog.URI) as Uri
        }
        if (uri != null)
            viewModel.setUri(EditContactViewModel.Image.Gallery(uri))
    }

    private fun observe() {
        viewModel.contact.observe(this) {
            binding.etGivenName.setText(it.givenName)
            binding.etFamilyName.setText(it.familyName)
            binding.etMiddleName.setText(it.middleName)
            binding.etPhone.setText(it.contact.phoneNumber)
            binding.etEmail.setText(it.email)
            binding.etAddress.setText(it.address)
            Glide.with(binding.ivAvatar)
                .load(it.contact.avatar)
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
            when (it) {
                is EditContactViewModel.Image.Camera -> binding.ivAvatar.setImageBitmap(it.bitmap)
                is EditContactViewModel.Image.Gallery -> binding.ivAvatar.setImageURI(it.uri)
            }
        }
    }

    override fun onClick(view: View?) {
        when (view) {
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