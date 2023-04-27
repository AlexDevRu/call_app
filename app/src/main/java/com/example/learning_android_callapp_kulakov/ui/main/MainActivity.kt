package com.example.learning_android_callapp_kulakov.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.learning_android_callapp_kulakov.R
import com.example.learning_android_callapp_kulakov.databinding.ActivityMainBinding
import com.example.learning_android_callapp_kulakov.ui.adapters.TabAdapter
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val tabAdapter = TabAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pager.adapter = tabAdapter

        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.setText(if (position == 0) R.string.phone else R.string.contacts)
        }.attach()
    }
}