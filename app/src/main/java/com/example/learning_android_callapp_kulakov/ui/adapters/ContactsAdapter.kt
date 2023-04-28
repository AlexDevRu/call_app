package com.example.learning_android_callapp_kulakov.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.learning_android_callapp_kulakov.R
import com.example.learning_android_callapp_kulakov.Utils
import com.example.learning_android_callapp_kulakov.databinding.ItemContactBinding
import com.example.learning_android_callapp_kulakov.models.Contact

class ContactsAdapter(
    private val listener: Listener
): ListAdapter<Contact, ContactsAdapter.ContactViewHolder>(DIFF_UTIL) {

    companion object {
        val DIFF_UTIL = object : ItemCallback<Contact>() {
            override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
                return oldItem == newItem
            }
        }
    }

    interface Listener {
        fun onItemClick(contact: Contact)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContactViewHolder(
        private val binding: ItemContactBinding
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private var contact: Contact? = null

        init {
            binding.root.setOnClickListener(this)
            binding.btnCall.setOnClickListener(this)
        }

        fun bind(contact: Contact) {
            this.contact = contact
            Glide.with(binding.ivAvatar)
                .load(contact.avatar)
                .error(R.drawable.ic_account)
                .into(binding.ivAvatar)
            binding.tvDisplayName.text = contact.name
            binding.btnCall.isVisible = !contact.phoneNumber.isNullOrBlank()
        }

        override fun onClick(view: View?) {
            when (view) {
                binding.root -> listener.onItemClick(contact!!)
                binding.btnCall -> Utils.doCall(binding.root.context, contact!!.phoneNumber!!)
            }
        }

    }

}