package com.example.learning_android_callapp_kulakov

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.learning_android_callapp_kulakov.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.all { it.value }) {
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

        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestRole()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestRole() {
        val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        screeningLauncher.launch(intent)
    }
}