package com.example.learning_android_callapp_kulakov.ui.phone

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learning_android_callapp_kulakov.Extensions.pop
import com.example.learning_android_callapp_kulakov.Utils
import com.example.learning_android_callapp_kulakov.databinding.FragmentPhoneBinding
import com.example.learning_android_callapp_kulakov.models.Call
import com.example.learning_android_callapp_kulakov.ui.adapters.CallLogAdapter

class PhoneFragment : Fragment(), View.OnClickListener, View.OnLongClickListener, CallLogAdapter.Listener {

    private lateinit var binding: FragmentPhoneBinding

    private val viewModel by viewModels<PhoneViewModel>()

    private val callLogAdapter = CallLogAdapter(this)

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.READ_CALL_LOG] == true) {
            viewModel.readCallLog()
        }
    }

    private val callPhonePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        if (result) {
            Utils.doCall(requireContext(), viewModel.phoneNumber.orEmpty())
        }
    }

    private val screeningLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            Toast.makeText(requireContext(), "Call screening is enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private val dialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            Toast.makeText(requireContext(), "App is default for calls", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = requireContext().getSystemService(AppCompatActivity.ROLE_SERVICE) as RoleManager
            dialerLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, requireContext().packageName)
            dialerLauncher.launch(intent)
        }

        val permissions = mutableListOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
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

        binding.rvCalls.adapter = callLogAdapter

        val dividerItemDecoration = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        binding.rvCalls.addItemDecoration(dividerItemDecoration)

        observe()
    }

    private fun observe() {
        viewModel.calls.observe(viewLifecycleOwner) {
            callLogAdapter.submitList(it) {
                binding.rvCalls.scrollToPosition(0)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestRole() {
        val roleManager = requireContext().getSystemService(AppCompatActivity.ROLE_SERVICE) as RoleManager
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
            binding.btnCall -> {
                viewModel.phoneNumber = binding.etPhoneNumber.text.toString().trim()
                callPhonePermissionsLauncher.launch(Manifest.permission.CALL_PHONE)
            }
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

    override fun onItemClick(call: Call) {
        viewModel.phoneNumber = call.phoneNumber
        callPhonePermissionsLauncher.launch(Manifest.permission.CALL_PHONE)
    }
}