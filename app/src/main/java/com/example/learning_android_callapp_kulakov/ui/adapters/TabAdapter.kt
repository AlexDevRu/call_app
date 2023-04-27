package com.example.learning_android_callapp_kulakov.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.learning_android_callapp_kulakov.ui.contacts.ContactsFragment
import com.example.learning_android_callapp_kulakov.ui.phone.PhoneFragment

class TabAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PhoneFragment()
            else -> ContactsFragment()
        }
    }

}