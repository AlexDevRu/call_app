package com.example.learning_android_callapp_kulakov.ui.contacts

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learning_android_callapp_kulakov.databinding.FragmentContactsBinding
import com.example.learning_android_callapp_kulakov.models.Contact
import com.example.learning_android_callapp_kulakov.ui.adapters.ContactsAdapter
import com.example.learning_android_callapp_kulakov.ui.add_contact.AddContactActivity
import com.example.learning_android_callapp_kulakov.ui.contact_details.ContactDetailsActivity

class ContactsFragment : Fragment(), ContactsAdapter.Listener, View.OnClickListener {

    private lateinit var binding: FragmentContactsBinding

    private val viewModel by viewModels<ContactsViewModel>()

    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            viewModel.readContacts()
        }
    }

    private val contactsAdapter = ContactsAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvContacts.adapter = contactsAdapter
        val dividerItemDecoration = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        binding.rvContacts.addItemDecoration(dividerItemDecoration)
        binding.rvContacts.setHasFixedSize(true)
        binding.fabAddContact.setOnClickListener(this)
        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        observe()
    }

    private fun observe() {
        viewModel.contacts.observe(viewLifecycleOwner) {
            contactsAdapter.submitList(it)
        }
    }

    override fun onClick(view: View?) {
        when (view) {
            binding.fabAddContact -> AddContactActivity.startActivity(requireContext())
        }
    }

    override fun onItemClick(contact: Contact) {
        ContactDetailsActivity.startActivity(requireContext(), contact.id)
    }
}