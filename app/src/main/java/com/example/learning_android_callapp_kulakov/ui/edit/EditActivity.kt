package com.example.learning_android_callapp_kulakov.ui.edit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.learning_android_callapp_kulakov.databinding.ActivityEditContactBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityEditContactBinding

    private val viewModel by viewModels<EditContactViewModel>()

    private lateinit var uri: Uri

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            /*val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.data?.extras?.getParcelable("data", Bitmap::class.java)
            } else {
                it.data?.extras?.get("data") as Bitmap
            }
            if (imageBitmap != null) {
                viewModel.setUri(EditContactViewModel.Image.Camera(imageBitmap))
            }*/
            viewModel.setUri(EditContactViewModel.Image.Gallery(uri))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabSave.setOnClickListener(this)
        binding.ivAvatar.setOnClickListener(this)

        observe()
    }

    private fun observe() {
        viewModel.contact.observe(this) {
            binding.etFullName.setText(it.contact.name)
            binding.etPhone.setText(it.contact.phoneNumber)
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
            binding.fabSave -> saveChanges()
            binding.ivAvatar -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(System.currentTimeMillis())
                val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val file = File.createTempFile("JPEG_${timeStamp}_",".jpg", storageDir)

                uri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", file)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                cameraLauncher.launch(intent)
            }
        }
    }

    private fun saveChanges() {
        val name = binding.etFullName.text?.toString().orEmpty()
        val phone = binding.etPhone.text?.toString().orEmpty()
        viewModel.saveChanges(name, phone)
    }

    companion object {
        fun startActivity(context: Context, contactId: Long) {
            val intent = Intent(context, EditActivity::class.java)
                .putExtra(EditContactViewModel.CONTACT_ID, contactId)
            context.startActivity(intent)
        }
    }
}