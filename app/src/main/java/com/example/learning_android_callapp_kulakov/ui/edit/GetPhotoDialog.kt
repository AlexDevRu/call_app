package com.example.learning_android_callapp_kulakov.ui.edit

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.learning_android_callapp_kulakov.R
import com.example.learning_android_callapp_kulakov.databinding.DialogGetPhotoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GetPhotoDialog : DialogFragment(), DialogInterface.OnClickListener, View.OnClickListener {

    private lateinit var uri: Uri

    private lateinit var binding: DialogGetPhotoBinding

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            setFragmentResult(requestKey, bundleOf(URI to uri))
            dismiss()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            val uri = it.data?.data ?: return@registerForActivityResult
            setFragmentResult(requestKey, bundleOf(URI to uri))
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogGetPhotoBinding.inflate(layoutInflater)
        binding.tvCamera.setOnClickListener(this)
        binding.tvGallery.setOnClickListener(this)

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_option)
            .setView(binding.root)
            .create()
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.tvCamera -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(System.currentTimeMillis())
                val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val file = File.createTempFile("JPEG_${timeStamp}_",".jpg", storageDir)

                uri = FileProvider.getUriForFile(requireContext(), "com.example.android.fileprovider", file)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                cameraLauncher.launch(intent)
            }
            binding.tvGallery -> {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher.launch(intent)
            }
        }
    }

    override fun onClick(p0: DialogInterface?, which: Int) {
        when (which) {
            0 -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(System.currentTimeMillis())
                val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val file = File.createTempFile("JPEG_${timeStamp}_",".jpg", storageDir)

                uri = FileProvider.getUriForFile(requireContext(), "com.example.android.fileprovider", file)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                cameraLauncher.launch(intent)
            }
            1 -> {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher.launch(intent)
            }
        }
    }

    companion object {
        const val requestKey = "GetPhotoDialog"
        const val URI = "uri"
    }

}