package com.example.learning_android_callapp_kulakov.ui.add_contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.learning_android_callapp_kulakov.databinding.ActivityAddContactBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddContactActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityAddContactBinding

    private val viewModel by viewModels<AddContactViewModel>()

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            val uri = it.data?.data ?: return@registerForActivityResult
            viewModel.setUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivAvatar.setOnClickListener(this)
        binding.fabSave.setOnClickListener(this)
        observe()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.goBack.collectLatest {
                    finish()
                }
            }
        }
        viewModel.uri.observe(this) {
            binding.ivAvatar.setImageURI(it)
        }
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.ivAvatar -> openGalleryIntent()
            binding.fabSave -> {
                viewModel.insertContact(
                    binding.etFullName.text?.toString().orEmpty(),
                    binding.etPhone.text?.toString().orEmpty()
                )
            }
        }
    }

    private fun openGalleryIntent() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, AddContactActivity::class.java)
            context.startActivity(intent)
        }
    }
}