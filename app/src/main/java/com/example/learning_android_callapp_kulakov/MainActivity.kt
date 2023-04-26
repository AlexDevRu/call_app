package com.example.learning_android_callapp_kulakov

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.learning_android_callapp_kulakov.Extensions.pop
import com.example.learning_android_callapp_kulakov.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var binding: ActivityMainBinding

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            Toast.makeText(this, "All permissions are granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val screeningLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            Toast.makeText(this, "Call screening is enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private val dialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            Toast.makeText(this, "App is default for calls", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAppAsDefault.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = getSystemService(ROLE_SERVICE) as RoleManager
                dialerLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            } else {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                dialerLauncher.launch(intent)
            }
        }

        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionsLauncher.launch(permissions.toTypedArray())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestRole()
        }

        binding.btn1.setOnClickListener(this)
        binding.btn2.setOnClickListener(this)
        binding.btn3.setOnClickListener(this)
        binding.btn4.setOnClickListener(this)
        binding.btn5.setOnClickListener(this)
        binding.btn6.setOnClickListener(this)
        binding.btn7.setOnClickListener(this)
        binding.btn8.setOnClickListener(this)
        binding.btn9.setOnClickListener(this)
        binding.btnStar.setOnClickListener(this)
        binding.btn0.setOnClickListener(this)
        binding.btn0.setOnLongClickListener(this)
        binding.btnSharp.setOnClickListener(this)
        binding.btnBackspace.setOnClickListener(this)
        binding.btnCall.setOnClickListener(this)

        binding.etPhoneNumber.doAfterTextChanged {
            binding.btnBackspace.isVisible = !it.isNullOrEmpty()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestRole() {
        val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        screeningLauncher.launch(intent)
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.btn1 -> binding.etPhoneNumber.append("1")
            binding.btn2 -> binding.etPhoneNumber.append("2")
            binding.btn3 -> binding.etPhoneNumber.append("3")
            binding.btn4 -> binding.etPhoneNumber.append("4")
            binding.btn5 -> binding.etPhoneNumber.append("5")
            binding.btn6 -> binding.etPhoneNumber.append("6")
            binding.btn7 -> binding.etPhoneNumber.append("7")
            binding.btn8 -> binding.etPhoneNumber.append("8")
            binding.btn9 -> binding.etPhoneNumber.append("9")
            binding.btnStar -> binding.etPhoneNumber.append("*")
            binding.btn0 -> binding.etPhoneNumber.append("0")
            binding.btnSharp -> binding.etPhoneNumber.append("#")
            binding.btnBackspace -> binding.etPhoneNumber.pop()
            binding.btnCall -> doCall()
        }
    }

    override fun onLongClick(view: View?): Boolean {
        return when (view) {
            binding.btn0 -> {
                binding.etPhoneNumber.append("+")
                true
            }
            else -> false
        }
    }

    private fun doCall() {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:" + binding.etPhoneNumber.text.toString().trim())
        startActivity(intent)
    }
}