package com.example.learning_android_callapp_kulakov.ui.add_contact

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.learning_android_callapp_kulakov.databinding.ActivityAddContactBinding
import com.example.learning_android_callapp_kulakov.ui.edit.GetPhotoDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddContactActivity : AppCompatActivity(), View.OnClickListener, FragmentResultListener {

    private lateinit var binding: ActivityAddContactBinding

    private val viewModel by viewModels<AddContactViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivAvatar.setOnClickListener(this)
        binding.fabSave.setOnClickListener(this)
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
            viewModel.setUri(uri)
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
            binding.ivAvatar -> {
                val getPhotoDialog = GetPhotoDialog()
                getPhotoDialog.show(supportFragmentManager, null)
            }
            binding.fabSave -> {
                viewModel.insertContact(
                    binding.etGivenName.text?.toString().orEmpty(),
                    binding.etFamilyName.text?.toString().orEmpty(),
                    binding.etMiddleName.text?.toString().orEmpty(),
                    binding.etPhone.text?.toString().orEmpty(),
                    binding.etEmail.text?.toString().orEmpty(),
                    binding.etAddress.text?.toString().orEmpty()
                )
            }
        }
    }

    companion object {
        fun startActivity(context: Context) {
            val intent = Intent(context, AddContactActivity::class.java)
            context.startActivity(intent)
        }
    }
}